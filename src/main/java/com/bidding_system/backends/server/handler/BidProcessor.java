package com.bidding_system.backends.server.handler;

import com.bidding_system.backends.common.messages.MsgBid.PlaceBidFailed;
import com.bidding_system.backends.common.messages.MsgBid.ReceiveMaxBidder;
import com.bidding_system.backends.common.messages.MsgBid.ServerBidRespond;
import com.bidding_system.backends.common.models.accounts.User;
import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.common.models.bidding.BidTransaction;
import com.bidding_system.backends.common.models.core.Item;
import com.bidding_system.backends.common.models.items.ItemFactory;
import com.bidding_system.backends.common.models.items.ItemType;
import com.bidding_system.backends.server.database.BidTransactionDAO;
import com.bidding_system.backends.server.database.UserDAO;
import com.bidding_system.backends.server.policy.IncrementPolicy;
import com.bidding_system.backends.server.service.AuctionService;
import com.google.gson.Gson;

import java.util.concurrent.LinkedBlockingQueue;

public class BidProcessor {

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static BidProcessor instance;

    public static synchronized BidProcessor getInstance() {
        if (instance == null) instance = new BidProcessor();
        return instance;
    }

    private BidProcessor() {
        Thread worker = new Thread(this::processLoop, "bid-worker");
        worker.setDaemon(true);
        worker.start();
    }

    // ── Unified BidRequest — không phân biệt manual hay auto ──────────────────
    public record BidRequest(String userId, String auctionId, double amount) {}

    // ── Queue ──────────────────────────────────────────────────────────────────
    private final LinkedBlockingQueue<BidRequest> queue = new LinkedBlockingQueue<>();

    // ── Public API — tất cả bid đều đi qua đây ────────────────────────────────
    public void submit(String userId, String auctionId, double amount) {
        queue.offer(new BidRequest(userId, auctionId, amount));
    }

    // ── Worker loop ────────────────────────────────────────────────────────────
    private void processLoop() {
        while (true) {
            try {
                BidRequest req = queue.take();
                process(req);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[BidProcessor] Unexpected error: " + e.getMessage());
            }
        }
    }

    // ── Single algorithm — xử lý đồng nhất mọi bid ────────────────────────────
    private void process(BidRequest req) {
        try {
            BidTransactionDAO bidDAO = new BidTransactionDAO();
            UserDAO userDAO = new UserDAO();

            // 1. Lấy giá cao nhất hiện tại
            ServerBidRespond currentMax = bidDAO.getMaxBidder(req.auctionId());
            double currentMaxAmount = (currentMax != null) ? currentMax.amount : 0;

            Auction auction = AuctionService.getManagedActiveAuctionByAuctionId(req.auctionId());
            double startingPrice = (auction != null) ? auction.getItem().getPrices() : 0;
            double floorPrice = Math.max(currentMaxAmount, startingPrice);

            if (req.amount() <= floorPrice) {
                notifyBidFailed(req.userId(), "Your bid must higher than " + floorPrice);
                return;
            }

            // 3. Lưu bid vào DB
            User bidUser = userDAO.getUser(req.userId());
            Item dummyItem = ItemFactory.createItem(ItemType.Art, "auction-item", 0, "");
            bidDAO.saveBid(req.auctionId(), new BidTransaction(bidUser, dummyItem, req.amount()));

            // 4. Hỏi AutoBidEngine: có ai cần counter không?
            //    Nếu có → submit lại vào queue như bid thường, worker xử lý tiếp
            AutoBidEngine.getInstance()
                    .calculateNextBid(req.auctionId(), req.userId(), req.amount())
                    .ifPresent(next -> submit(next.userId(), next.auctionId(), next.amount()));

            // 5. Broadcast giá hiện tại sau khi bid vừa được lưu
            //    (auto counter nếu có sẽ broadcast thêm một lần nữa sau khi được xử lý)
            ServerBidRespond newMax = bidDAO.getMaxBidder(req.auctionId());
            broadcastMaxBidder(req.auctionId(), newMax);

            // 6. Kiểm tra anti-snipe
            Auction managedAuction = AuctionService.getManagedActiveAuctionByAuctionId(req.auctionId());
            if (managedAuction != null) {
                boolean extended = AuctionService.extendAuctionIfNeeded(managedAuction.getItem().getId());
                if (extended) {
                    ServerAuctionManager.getInstance().broadcastAuctionExtended(managedAuction);
                }
            }

        } catch (Exception e) {
            System.err.println("[BidProcessor] Error processing bid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────
    private void broadcastMaxBidder(String auctionId, ServerBidRespond max) {
        if (max == null) return;
        max.auctionId = auctionId;
        String json = new Gson().toJson(new ReceiveMaxBidder(max,IncrementPolicy.getIncrement(max.amount)));
        AuctionRoom.getInstance().broadcast(json);
    }

    private void notifyBidFailed(String userId, String reason) {
        String json = new Gson().toJson(new PlaceBidFailed(reason));
        AuctionRoom.getInstance().sendToUser(userId, json);
    }
}