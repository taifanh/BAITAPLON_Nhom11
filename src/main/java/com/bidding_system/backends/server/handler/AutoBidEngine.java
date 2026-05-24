package com.bidding_system.backends.server.handler;

import com.bidding_system.backends.common.messages.MsgBid.PlaceBidFailed;
import com.bidding_system.backends.common.messages.MsgBid.ServerBidRespond;
import com.bidding_system.backends.server.database.BidTransactionDAO;
import com.bidding_system.backends.server.handler.BidProcessor.BidRequest;
import com.bidding_system.backends.server.policy.IncrementPolicy;
import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoBidEngine {

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
        // 1. Thêm entry vào registry
        autoBids.computeIfAbsent(entry.auctionId(),
                _ -> Collections.synchronizedList(new ArrayList<>())).add(entry);

        try {
            BidTransactionDAO db = new BidTransactionDAO();

            // 2. Lấy giá hiện tại từ DB
            ServerBidRespond currentMax = db.getMaxBidder(entry.auctionId());
            double currentMaxAmount = (currentMax != null) ? currentMax.amount : 0;

            double minValidMaxBid = currentMaxAmount + IncrementPolicy.getIncrement(currentMaxAmount);

            if (entry.maxBid() < minValidMaxBid) {
                // Thông báo cho client: maxBid phải >= minValidMaxBid
                AuctionRoom.getInstance().sendToUser(entry.userId(),
                        new Gson().toJson(new PlaceBidFailed("maxBid phải ít nhất " + minValidMaxBid + "đ")));
                // Xoá entry vừa thêm vào
                remove(entry.auctionId(), entry.userId());
                return;
            }

            // 3. Tìm rival mạnh nhất (auto-bidder khác có maxBid cao nhất)
            List<AutoBidEntry> entries = autoBids.get(entry.auctionId());
            Optional<AutoBidEntry> topRival;
            synchronized (entries) {
                topRival = entries.stream()
                        .filter(e -> !e.userId().equals(entry.userId()))
                        .max(Comparator.comparingDouble(AutoBidEntry::maxBid));
            }

            // 4. Tính giá tối ưu theo Proxy Bidding
            //    Nếu có rival → bid vừa đủ vượt rival.maxBid
            //    Nếu không    → bid vừa đủ vượt currentMax
            double basePrice = topRival
                    .map(AutoBidEntry::maxBid)
                    .orElse(currentMaxAmount);

            double inc = IncrementPolicy.getIncrement(basePrice);
            double targetPrice = Math.min(entry.maxBid(), basePrice + inc);

            // 5. Submit vào BidProcessor — từ đây xử lý như bid thủ công
            if (targetPrice > currentMaxAmount) {
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