package com.bidding_system.backends.server.handler;

import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.server.database.BidTransactionDAO;
import com.bidding_system.backends.server.database.UserDAO;
import com.bidding_system.backends.server.service.AuctionService;
import com.google.gson.Gson;
import com.bidding_system.backends.common.messages.MsgBid.ReceiveMaxBidder;
import com.bidding_system.backends.common.messages.MsgBid.ServerBidRespond;
import com.bidding_system.backends.common.models.bidding.BidTransaction;
import com.bidding_system.backends.common.models.core.Item;
import com.bidding_system.backends.common.models.items.ItemType;
import com.bidding_system.backends.common.models.items.ItemFactory;
import com.bidding_system.backends.common.models.accounts.User;

import java.util.*;
import java.util.concurrent.*;

public class BidBatchProcessor {

    private static BidBatchProcessor instance;
    public static synchronized BidBatchProcessor getInstance() {
        if (instance == null) instance = new BidBatchProcessor();
        return instance;
    }

    public record PendingBid(String userId, String auctionId, double amount, long receivedAt) {}


    private final ConcurrentHashMap<String, List<PendingBid>> pendingBids = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int BATCH_INTERVAL_SECONDS = 1;

    private BidBatchProcessor() {
        scheduler.scheduleAtFixedRate(
                this::flushAllBatches,
                BATCH_INTERVAL_SECONDS,   // delay trước batch đầu tiên
                BATCH_INTERVAL_SECONDS,   // interval
                TimeUnit.SECONDS
        );
    }

    public void submitBid(String userId, String auctionId, double amount) {
        PendingBid bid = new PendingBid(userId, auctionId, amount,
                System.currentTimeMillis());
        pendingBids.computeIfAbsent(auctionId, id -> Collections.synchronizedList(new ArrayList<>())).add(bid);
    }

    public synchronized void flushAuction(String auctionId) {
        List<PendingBid> batch = pendingBids.remove(auctionId);
        if (batch != null && !batch.isEmpty()) {
            flushManualBids(auctionId, batch);
        }
    }

    private void flushAllBatches() {
        if (pendingBids.isEmpty()) return;

        // Snapshot & clear atomically per auctionId
        for (String auctionId : new HashSet<>(pendingBids.keySet())) {
            List<PendingBid> batch = pendingBids.remove(auctionId);
            if (batch == null || batch.isEmpty()) continue;
            flushManualBids(auctionId, batch);
        }
    }

    // ── Xử lý 1 batch của 1 auctionId ────────────────────────────
    private void flushManualBids(String auctionId, List<PendingBid> batch) {

        try {
            BidTransactionDAO db = new BidTransactionDAO();
            UserDAO userDAO = new UserDAO();

            // Tìm max bid trong batch (nếu tie → ưu tiên bid đến sớm hơn)
            PendingBid winner = batch.stream()
                    .max(Comparator
                            .comparingDouble(PendingBid::amount)
                            .thenComparingLong(b -> -b.receivedAt())) // receivedAt nhỏ hơn = sớm hơn
                    .orElse(null);

            if (winner == null) return;

            // Lấy max bid hiện tại trong DB để kiểm tra hợp lệ
            ServerBidRespond currentMax;
            try {
                currentMax = db.getMaxBidder(auctionId);
            } catch (Exception e) {
                currentMax = null;
            }

            double currentMaxAmount = (currentMax != null) ? currentMax.amount : 0;
            // Tìm max bid trong batch (nếu tie → ưu tiên bid đến sớm hơn)
            double batchMax = batch.stream()
                    .mapToDouble(PendingBid::amount)
                    .max()
                    .orElse(0);

            // Không có bid nào trong batch vượt DB → broadcast giá hiện tại rồi thôi
            if (batchMax <= currentMaxAmount) {
                broadcastMaxBidder(auctionId, currentMax);
                return;
            }

            // Lưu tất cả bid hợp lệ trong batch vào DB
            Auction managedAuction = AuctionService.getManagedActiveAuctionByAuctionId(auctionId);
            Item auctionItem = managedAuction != null
                    ? managedAuction.getItem()
                    : ItemFactory.createItem(ItemType.Art, "auction-item", 0, "");

            for (PendingBid bid : batch) {
                if (bid.amount() > currentMaxAmount) { // chỉ lưu bid hợp lệ
                    User bidUser = userDAO.getUser(bid.userId());
                    db.saveBid(auctionId,
                            new BidTransaction(bidUser, auctionItem, bid.amount()));
                }
            }

            // Broadcast kết quả batch
            AutoBidEngine.getInstance().resolveAuction(auctionId);
            ServerBidRespond result = db.getMaxBidder(auctionId);
            broadcastMaxBidder(auctionId, result);
            if (managedAuction != null) {
                boolean extended = AuctionService.extendAuctionIfNeeded(managedAuction.getItem().getId());
                if (extended) {
                    ServerAuctionManager.getInstance().broadcastAuctionExtended(managedAuction);
                }
            }

        } catch (Exception e) {
            System.err.println("[BidBatchProcessor] Error processing batch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Broadcast max bidder tới tất cả client đang watch auction ─
    private void broadcastMaxBidder(String auctionId, ServerBidRespond maxBidder) {
        if (maxBidder == null) return;
        maxBidder.auctionId = auctionId;
        ReceiveMaxBidder msg = new ReceiveMaxBidder(maxBidder);

        String json = new Gson().toJson(msg);
        AuctionRoom.getInstance().broadcast(json);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
