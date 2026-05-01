package controllers.Server;

import Database.BidTransactions;
import Database.UserStore;
import com.google.gson.Gson;
import models.Extra.messages.ReceiveMaxBidder;
import models.Extra.messages.ServerBidRespond;
import models.bidding.BidTransaction;
import models.core.Item;
import models.items.ItemType;
import models.items.itemFactory;
import models.accounts.User;

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
    private static final int BATCH_INTERVAL_SECONDS = 5;

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

        pendingBids
                .computeIfAbsent(auctionId, id -> Collections.synchronizedList(new ArrayList<>()))
                .add(bid);

    }

    private void flushAllBatches() {
        if (pendingBids.isEmpty()) return;

        // Snapshot & clear atomically per auctionId
        for (String auctionId : new HashSet<>(pendingBids.keySet())) {
            List<PendingBid> batch = pendingBids.remove(auctionId);
            if (batch == null || batch.isEmpty()) continue;
            processBatch(auctionId, batch);
        }
    }

    // ── Xử lý 1 batch của 1 auctionId ────────────────────────────
    private void processBatch(String auctionId, List<PendingBid> batch) {

        try {
            BidTransactions db = new BidTransactions();
            UserStore userStore = new UserStore();

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
                currentMax = null; // chưa có bid nào trong DB
            }

            double currentMaxAmount = (currentMax != null) ? currentMax.amount : 0;

            if (winner.amount() <= currentMaxAmount) {
                broadcastMaxBidder(auctionId, currentMax);
                return;
            }

            // Lưu tất cả bid hợp lệ trong batch vào DB
            User winnerUser = userStore.getUser(winner.userId());
            Item dummyItem = itemFactory.createItem(ItemType.Art, "auction-item", 0, "");

            for (PendingBid bid : batch) {
                if (bid.amount() > currentMaxAmount) { // chỉ lưu bid hợp lệ
                    User bidUser = userStore.getUser(bid.userId());
                    db.saveBid(auctionId,
                            new BidTransaction(bidUser, dummyItem, bid.amount()));
                }
            }

            // Broadcast kết quả batch
            ServerBidRespond result = db.getMaxBidder(auctionId);
            broadcastMaxBidder(auctionId, result);

        } catch (Exception e) {
            System.err.println("[BidBatchProcessor] Error processing batch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Broadcast max bidder tới tất cả client đang watch auction ─
    private void broadcastMaxBidder(String auctionId, ServerBidRespond maxBidder) {
        if (maxBidder == null) return;
        ReceiveMaxBidder msg = new ReceiveMaxBidder(maxBidder);
        String json = new Gson().toJson(msg);
        AuctionRoom.getInstance().broadcast(json);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}