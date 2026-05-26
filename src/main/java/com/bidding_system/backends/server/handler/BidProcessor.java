package com.bidding_system.backends.server.handler;

import com.bidding_system.backends.common.messages.MsgBid.*;
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;

public class BidProcessor {
    BidTransactionDAO bidDAO = new BidTransactionDAO();
    UserDAO userDAO = new UserDAO();
    Item dummyItem = ItemFactory.createItem(ItemType.Art, "auction-item", 0, "");

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static BidProcessor instance;

    public static synchronized BidProcessor getInstance() throws IOException {
        if (instance == null) instance = new BidProcessor();
        return instance;
    }

    private BidProcessor() throws IOException {
        Thread worker = new Thread(this::processLoop, "bid-worker");
        worker.setDaemon(true);
        worker.start();
    }

    // ── BidRequest ─────────────────────────────────────────────────────────────
    public record BidRequest(
            String userId,
            String auctionId,
            double amount,   // manual: giá đặt
            boolean isAuto,
            double maxBid    // chỉ dùng khi isAuto = true
    ) {
        public static BidRequest manual(String userId, String auctionId, double amount) {
            return new BidRequest(userId, auctionId, amount, false, 0);
        }

        public static BidRequest auto(String userId, String auctionId, double maxBid) {
            return new BidRequest(userId, auctionId, 0, true, maxBid);
        }

        public static BidRequest cancelAuto(String userId, String auctionId) {
            return new BidRequest(userId, auctionId, 0, false, -1);
        }
    }

    // ── Queue ──────────────────────────────────────────────────────────────────
    private final LinkedBlockingQueue<BidRequest> queue = new LinkedBlockingQueue<>();

    // ── Public API ─────────────────────────────────────────────────────────────
    public void submitManualBid(String userId, String auctionId, double amount) {
        queue.offer(BidRequest.manual(userId, auctionId, amount));
    }

    public void cancelAutoBid(String userId, String auctionId) {
        queue.offer(BidRequest.cancelAuto(userId, auctionId));
    }

    public void submitAutoBid(String userId, String auctionId, double maxBid) {
        queue.offer(BidRequest.auto(userId, auctionId, maxBid));
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

    // ── Core algorithm ─────────────────────────────────────────────────────────
    private void process(BidRequest request) throws IOException, SQLException {
        ServerBidRespond currentWinner = bidDAO.getMaxBidder(request.auctionId());
        double currentMaxAmount = (currentWinner != null) ? currentWinner.amount : 0;
        Auction auction = AuctionService.getManagedActiveAuctionByAuctionId(request.auctionId());
        double startingPrice = (auction != null) ? auction.getItem().getPrices() : 0;
        double floorPrice = Math.max(startingPrice, currentMaxAmount);
        if (!request.isAuto() && request.maxBid() == -1) {
            bidDAO.cancelAutoBid(request.auctionId(), request.userId());
            String json = new Gson().toJson(new AutoBiddingCancelled("Auto bid cancelled !"));
            AuctionRoom.getInstance().sendToUser(request.userId(), json);
            return;
        }
        if (currentWinner == null) {
            if (request.isAuto) {
                if (request.maxBid < startingPrice) {
                    notifyPlaceBidFailed(request.userId(), "Max bid must be at least: " + startingPrice);
                    return;
                }
                saveAndBroadcast(request.userId(), request.auctionId(),
                        startingPrice, true, request.maxBid());
            } else {
                if (request.amount() < floorPrice) {
                    notifyPlaceBidFailed(request.userId(), "Invalid bid amount");
                    return;
                }
                saveAndBroadcast(request.userId(), request.auctionId(),
                        request.amount(), false, 0);
            }
        }
        else {
            if (currentWinner != null && currentWinner.userId.equals(request.userId()) && request.isAuto) {
                // Chỉ cho phép cập nhật maxBid nếu cao hơn
                if (request.maxBid < currentWinner.maxBid) {
                    notifyAutoBidFailed(request.userId(),
                            "You are already the highest bidder.");
                    return;
                }
                // Cập nhật maxBid mới, không tăng giá
                bidDAO.updateMaxBid(request.auctionId(), request.userId(), request.maxBid());
                return;
            }
            if(request.isAuto && currentWinner.isAuto) {
                if (request.maxBid <= currentWinner.maxBid) {
                    notifyAutoBidFailed(request.userId(), "Your max bid is insufficient to win the current auto-bidder.");
                    double increment = IncrementPolicy.getIncrement(request.maxBid);
                    double newBidAmount = Math.min(currentWinner.maxBid, request.maxBid + increment);
                    saveAndBroadcast(currentWinner.userId, request.auctionId, newBidAmount, true, currentWinner.maxBid);
                }
                else if (request.maxBid > currentWinner.maxBid) {
                    double increment = IncrementPolicy.getIncrement(currentWinner.maxBid);
                    double newBidAmount = Math.min(request.maxBid, currentWinner.maxBid + increment);
                    saveAndBroadcast(request.userId(), request.auctionId, newBidAmount, true, request.maxBid());
                }
            }
            else if(!request.isAuto && currentWinner.isAuto) {
                if(request.amount > currentWinner.maxBid) {
                    double newBidAmount = request.amount;
                    saveAndBroadcast(request.userId, request.auctionId, newBidAmount, false, request.maxBid);
                }
                else {
                    double increment = IncrementPolicy.getIncrement(request.amount());
                    double autoFinal = Math.min(request.amount() + increment, currentWinner.maxBid);
                    saveAndBroadcast(currentWinner.userId, request.auctionId(), autoFinal, true, currentWinner.maxBid);
                    notifyPlaceBidFailed(request.userId(),
                            "Your bid was outbid automatically. Current price: " + autoFinal);
                }
            }
            else if(request.isAuto && !currentWinner.isAuto) {
                if(request.maxBid > currentWinner.amount) {
                    double increment = IncrementPolicy.getIncrement(currentWinner.amount);
                    double newBidAmount = Math.min(request.maxBid, currentWinner.amount + increment);
                    saveAndBroadcast(request.userId, request.auctionId, newBidAmount, true, request.maxBid);
                } else {
                    notifyAutoBidFailed(request.userId(), "Your max bid must exceed current bid of: " + currentWinner.amount);
                }
            }
            else {
                if(request.amount > currentWinner.amount) {
                    saveAndBroadcast(request.userId, request.auctionId, request.amount, false, request.maxBid);
                } else {
                    notifyPlaceBidFailed(request.userId(), "Your bid must exceed current highest bid of: " + currentWinner.amount);
                }
            }
        }
        checkAntiSnipe(request.auctionId());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void saveAndBroadcast(String userId, String auctionId,
                                  double amount, boolean isAuto, double maxBid) {
        try {
            User bidUser = userDAO.getUser(userId);
            BidTransaction bid = new BidTransaction(bidUser, dummyItem, amount);
            bid.setAuto(isAuto);
            bid.setMaxBid(maxBid);
            bidDAO.saveBid(auctionId, bid);
            ServerBidRespond newMax = bidDAO.getMaxBidder(auctionId);
            broadcastMaxBidder(auctionId, newMax);
        } catch (Exception e) {
            System.err.println("[BidProcessor] saveAndBroadcast error: " + e.getMessage());
        }
    }

    private void broadcastMaxBidder(String auctionId, ServerBidRespond max) {
        if (max == null) return;
        max.auctionId = auctionId;
        String json = new Gson().toJson(
                new ReceiveMaxBidder(max, IncrementPolicy.getIncrement(max.amount)));
        AuctionRoom.getInstance().broadcast(json);
    }

    private void notifyPlaceBidFailed(String userId, String reason) {
        String json = new Gson().toJson(new PlaceBidFailed(reason));
        AuctionRoom.getInstance().sendToUser(userId, json);
    }

    private void notifyAutoBidFailed(String userId, String reason) {
        String json = new Gson().toJson(new AutoBidFailed(reason));
        AuctionRoom.getInstance().sendToUser(userId, json);
    }

    private void checkAntiSnipe(String auctionId) {
        Auction managed = AuctionService
                .getManagedActiveAuctionByAuctionId(auctionId);
        if (managed != null) {
            boolean extended = false;
            try {
                extended = AuctionService.extendAuctionIfNeeded(managed.getItem().getId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (extended) {
                ServerAuctionManager.getInstance()
                        .broadcastAuctionExtended(managed);
            }
        }
    }
}