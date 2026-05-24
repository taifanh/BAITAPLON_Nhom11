package com.bidding_system.backends.server.handler;

import com.bidding_system.backends.common.messages.MsgBid.AutoBiddingCancelled;
import com.bidding_system.backends.common.messages.MsgBid.PlaceBidFailed;
import com.bidding_system.backends.common.messages.MsgBid.ServerBidRespond;
import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.server.database.BidTransactionDAO;
import com.bidding_system.backends.server.handler.BidProcessor.BidRequest;
import com.bidding_system.backends.server.policy.IncrementPolicy;
import com.bidding_system.backends.server.service.AuctionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoBidEngine {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    // ── Singleton ──────────────────────────────────────────────────────────────
    private static AutoBidEngine instance;

    public static synchronized AutoBidEngine getInstance() {
        if (instance == null) instance = new AutoBidEngine();
        return instance;
    }

    private AutoBidEngine() {}

    // ── AutoBidEntry — thông tin đăng ký auto-bid của một user ────────────────
    public record AutoBidEntry(
            String userId,
            String auctionId,
            double maxBid,       // giá tối đa user chấp nhận
            long registeredAt    // ưu tiên ai đăng ký trước khi cùng maxBid
    ) {}

    // ── Registry — mỗi auction có một danh sách auto-bidder riêng ─────────────
    private final ConcurrentHashMap<String, List<AutoBidEntry>> autoBids = new ConcurrentHashMap<>();

    // ── register() ────────────────────────────────────────────────────────────
    // Được gọi khi user đăng ký auto-bid.
    // Tính giá tối ưu ngay tại đây (Proxy Bidding) rồi submit vào BidProcessor.
    public void register(AutoBidEntry entry) {
        autoBids.computeIfAbsent(entry.auctionId(),
                _ -> Collections.synchronizedList(new ArrayList<>())).add(entry);

        try {
            BidTransactionDAO db = new BidTransactionDAO();

            // 2. Lấy giá hiện tại từ DB
            ServerBidRespond currentMax = db.getMaxBidder(entry.auctionId());
            double currentMaxAmount = (currentMax != null) ? currentMax.amount : 0;

            // Lấy startingPrice làm sàn khi chưa có ai bid
            Auction auction = AuctionService.getManagedActiveAuctionByAuctionId(entry.auctionId());
            double startingPrice = (auction != null) ? auction.getItem().getPrices() : 0;
            double floorPrice = Math.max(currentMaxAmount, startingPrice);

            // Validate maxBid phải đủ để vượt floorPrice
            double minValidMaxBid = floorPrice + IncrementPolicy.getIncrement(floorPrice);
            if (entry.maxBid() < minValidMaxBid) {
                AuctionRoom.getInstance().sendToUser(entry.userId(),
                        new Gson().toJson(new PlaceBidFailed("maxBid phải ít nhất " + minValidMaxBid)));
                remove(entry.auctionId(), entry.userId());
                return;
            }

            // 3. Tìm rival mạnh nhất
            List<AutoBidEntry> entries = autoBids.get(entry.auctionId());
            Optional<AutoBidEntry> topRival;
            synchronized (entries) {
                topRival = entries.stream()
                        .filter(e -> !e.userId().equals(entry.userId()))
                        .max(Comparator.comparingDouble(AutoBidEntry::maxBid));
            }

            // 4. Tính giá tối ưu — dùng floorPrice thay vì currentMaxAmount
            double basePrice = topRival
                    .map(AutoBidEntry::maxBid)
                    .orElse(floorPrice); // ← floorPrice thay vì currentMaxAmount

            double inc = IncrementPolicy.getIncrement(basePrice);
            double targetPrice = Math.min(entry.maxBid(), basePrice + inc);

            // 5. Submit nếu vượt được floorPrice
            if (targetPrice > floorPrice) { // ← so sánh với floorPrice
                BidProcessor.getInstance().submit(entry.userId(), entry.auctionId(), targetPrice);
            }

        } catch (Exception e) {
            System.err.println("[AutoBidEngine] Register failed: " + e.getMessage());
        }
    }

    // ── calculateNextBid() ────────────────────────────────────────────────────
    // Được gọi bởi BidProcessor.process() sau mỗi bid được lưu thành công.
    // Tính xem có auto-bidder nào cần counter không, trả về Optional<BidRequest>.
    // Không save DB, không broadcast — chỉ tính toán thuần túy.
    public Optional<BidRequest> calculateNextBid(
            String auctionId, String triggerUserId, double triggerAmount) {

        List<AutoBidEntry> entries = autoBids.get(auctionId);

        // Không có auto-bidder nào trong phiên này
        if (entries == null || entries.isEmpty()) return Optional.empty();

        synchronized (entries) {
            // 1. Lọc ứng viên hợp lệ:
            //    - Không phải người vừa bid (không counter chính mình)
            //    - Còn đủ maxBid để vượt triggerAmount
            List<AutoBidEntry> candidates = entries.stream()
                    .filter(e -> !e.userId().equals(triggerUserId))
                    .filter(e -> e.maxBid() > triggerAmount)
                    .sorted(Comparator.comparingDouble(AutoBidEntry::maxBid).reversed()
                            .thenComparingLong(AutoBidEntry::registeredAt))
                    .toList();

            // Không có ai đủ điều kiện counter
            if (candidates.isEmpty()) return Optional.empty();

            // 2. Winner là người có maxBid cao nhất
            AutoBidEntry winner = candidates.get(0);

            // 3. Rival của winner là người đứng thứ hai (để tính giá tối ưu)
            Optional<AutoBidEntry> rival = candidates.stream()
                    .filter(e -> !e.userId().equals(winner.userId()))
                    .findFirst();

            // 4. Tính giá tối ưu theo Proxy Bidding
            //    Nếu có rival → bid vừa đủ vượt rival.maxBid
            //    Nếu không    → bid vừa đủ vượt triggerAmount
            double basePrice = rival
                    .map(AutoBidEntry::maxBid)
                    .orElse(triggerAmount);

            double inc = IncrementPolicy.getIncrement(basePrice);
            double targetPrice = Math.min(winner.maxBid(), basePrice + inc);

            // targetPrice không vượt được trigger → không cần counter
            if (targetPrice <= triggerAmount) return Optional.empty();

            return Optional.of(new BidRequest(winner.userId(), auctionId, targetPrice));
        }
    }

    public void cancelExhaustedBidders(String auctionId, double currentAmount) {
        List<AutoBidEntry> entries = autoBids.get(auctionId);
        if (entries == null) return;

        List<AutoBidEntry> exhausted;
        synchronized (entries) {
            // Tìm những người maxBid <= giá hiện tại — không còn khả năng counter
            exhausted = entries.stream()
                    .filter(e -> e.maxBid() <= currentAmount)
                    .toList();

            // Xoá khỏi registry
            entries.removeIf(e -> e.maxBid() <= currentAmount);
        }
        Gson gson = new Gson();
        // Gửi thông báo cancel cho từng người
        for (AutoBidEntry e : exhausted) {
            AutoBiddingCancelled msg = new AutoBiddingCancelled();
            msg.message = "Your max bid is no longer sufficient";
            AuctionRoom.getInstance().sendToUser(e.userId(), gson.toJson(msg));
            System.out.printf("[AutoBidEngine] Exhausted | user=%s auction=%s maxBid=%.0f currentPrice=%.0f%n",
                    e.userId(), auctionId, e.maxBid(), currentAmount);
        }
    }

    // ── remove() ──────────────────────────────────────────────────────────────
    // Huỷ đăng ký auto-bid của một user trong một phiên cụ thể.
    // Gọi khi user chủ động tắt auto-bid.
    public void remove(String auctionId, String userId) {
        List<AutoBidEntry> entries = autoBids.get(auctionId);
        if (entries == null) return;
        entries.removeIf(e -> e.userId().equals(userId));
        System.out.printf("[AutoBidEngine] Removed | user=%s auction=%s%n", userId, auctionId);
    }

    // ── removeAll() ───────────────────────────────────────────────────────────
    // Xoá toàn bộ auto-bid của một phiên khi phiên kết thúc.
    // Gọi bởi AuctionManager khi auction chuyển sang trạng thái FINISHED.
    public void removeAll(String auctionId) {
        autoBids.remove(auctionId);
        System.out.printf("[AutoBidEngine] Cleared auction=%s%n", auctionId);
    }
}