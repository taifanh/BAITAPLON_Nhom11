package com.bidding_system.backends.server.handler;

import com.bidding_system.backends.common.messages.MsgBid.ReceiveMaxBidder;
import com.bidding_system.backends.common.messages.MsgBid.ServerBidRespond;
import com.bidding_system.backends.common.models.accounts.User;
import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.common.models.bidding.BidTransaction;
import com.bidding_system.backends.server.database.BidTransactionDAO;
import com.bidding_system.backends.server.database.UserDAO;
import com.bidding_system.backends.server.service.AuctionService;
import com.google.gson.Gson;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoBidEngine {
    private static AutoBidEngine instance;
    public static synchronized AutoBidEngine getInstance() {
        if (instance == null) instance = new AutoBidEngine();
        return instance;
    }
    private AutoBidEngine(){}
    public record AutoBidEntry(
            String userId,
            String auctionId,
            double maxBid,
            double increment,
            long registeredAt   // ưu tiên ai đăng ký trước khi tie
    ) {}
    record Candidate(
            String userId,
            double maxBid,
            double increment,
            long priority
    ) {}
    private final ConcurrentHashMap<String, List<AutoBidEntry>> autoBids = new ConcurrentHashMap<>();
    public void register(AutoBidEntry entry) {
        autoBids.computeIfAbsent(entry.auctionId(), _ -> Collections.synchronizedList(new ArrayList<>())).add(entry);
        resolveAuction(entry.auctionId());
    }
    public void removeAll(String auctionId) {
        autoBids.remove(auctionId);
    }
    public void remove(String auctionId, String userId) {
        List<AutoBidEntry> entries = autoBids.get(auctionId);
        if (entries == null) return;
        entries.removeIf(e -> e.userId().equals(userId));
        resolveAuction(auctionId);
        System.out.printf("[AutoBid] Cancelled | user=%s auction=%s%n", userId, auctionId);
    }

    public synchronized void resolveAuction(String auctionId) {
        try {
            BidTransactionDAO db = new BidTransactionDAO();
            ServerBidRespond currentMax = db.getMaxBidder(auctionId);
            List<AutoBidEntry> entries = autoBids.get(auctionId);

            List<Candidate> candidates = new ArrayList<>();
            boolean currentWinnerAlreadyAuto = false;

            if (entries != null && currentMax != null) {
                synchronized (entries) {
                    currentWinnerAlreadyAuto = entries.stream()
                            .anyMatch(e -> e.userId().equals(currentMax.userId));
                }
            }

            // Thêm người đang dẫn đầu thủ công nếu họ không phải auto-bidder
            if (currentMax != null && !currentWinnerAlreadyAuto) {
                candidates.add(new Candidate(currentMax.userId, currentMax.amount, 0, Long.MIN_VALUE));
            }

            if (entries != null) {
                synchronized (entries) {
                    for (AutoBidEntry e : entries) {
                        candidates.add(new Candidate(e.userId(), e.maxBid(), e.increment(), e.registeredAt()));
                    }
                }
            }

            if (candidates.isEmpty()) return;

            candidates.sort(Comparator.comparingDouble(Candidate::maxBid)
                    .reversed()
                    .thenComparingLong(Candidate::priority));

            Candidate winner = candidates.get(0);
            Candidate second = candidates.size() > 1 ? candidates.get(1) : null;

            Auction auction = AuctionService.getManagedActiveAuctionByAuctionId(auctionId);
            if (auction == null) {
                System.err.printf("[AutoBid] Auction %s not managed yet, skipping%n", auctionId);
                return;
            }

            double startingPrice = auction.getItem().getPrices();
            double currentMaxAmount = (currentMax != null) ? currentMax.amount : startingPrice;
            double finalAmount;

            if (second == null) {
                // Chỉ 1 bidder: bid ở mức baseline (không thấp hơn giá hiện tại)
                finalAmount = Math.min(winner.maxBid(), Math.max(startingPrice, currentMaxAmount));
            } else {
                // 2+ bidder: winner bid vừa đủ vượt second, nhưng không thấp hơn giá hiện tại
                double neededToBeat = Math.max(currentMaxAmount, second.maxBid() + winner.increment());
                finalAmount = Math.min(winner.maxBid(), neededToBeat);
            }

            // Kiểm tra xem đã resolved chưa
            boolean alreadyResolved = currentMax != null
                    && currentMax.userId.equals(winner.userId())
                    && currentMax.amount >= finalAmount; // >= thay vì == để tránh rebid không cần thiết
            if (alreadyResolved) return;
            UserDAO userStore = new UserDAO();

            User winnerUser =
                    userStore.getUser(winner.userId());

            db.saveBid(
                    auctionId,
                    new BidTransaction(
                            winnerUser,
                            auction.getItem(),
                            finalAmount
                    )
            );
            ServerBidRespond finalMax =
                    db.getMaxBidder(auctionId);

            if (finalMax == null) {
                return;
            }

            finalMax.auctionId = auctionId;

            String json =
                    new Gson().toJson(
                            new ReceiveMaxBidder(finalMax)
                    );

            AuctionRoom.getInstance().broadcast(json);

        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
