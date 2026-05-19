package backends.server.handler;

import backends.common.models.accounts.User;
import backends.common.models.bidding.BidTransaction;
import backends.common.models.core.Item;
import backends.common.models.items.ItemFactory;
import backends.common.models.items.ItemType;
import backends.server.database.BidTransactions;
import backends.server.database.UserStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoBidEngine {
    private static AutoBidEngine instance;
    public static synchronized AutoBidEngine getInstance() {
        if (instance == null) instance = new AutoBidEngine();
        return instance;
    }
    private AutoBidEngine(){};
    public record AutoBidEntry(
            String userId,
            String auctionId,
            double maxBid,
            double increment,
            long registeredAt   // ưu tiên ai đăng ký trước khi tie
    ) {}
    private final ConcurrentHashMap<String, List<AutoBidEntry>> autoBids = new ConcurrentHashMap<>();
    public void register(AutoBidEntry entry) {
        autoBids.computeIfAbsent(entry.auctionId(), id -> Collections.synchronizedList(new ArrayList<>())).add(entry);
        System.out.printf("[AutoBid] Registered | user=%s auction=%s max=%.0f inc=%.0f%n",
                entry.userId(), entry.auctionId(), entry.maxBid(), entry.increment());
    }
    public void removeAll(String auctionId) {
        autoBids.remove(auctionId);
    }
    public void triggerSync(String auctionId, double currentPrice, String currentWinnerId) {
        List<AutoBidEntry> entries = autoBids.get(auctionId);
        if (entries == null || entries.isEmpty()) return;

        Optional<AutoBidEntry> candidate = resolveBestCandidate(
                entries, currentPrice, currentWinnerId);

        if (candidate.isEmpty()) return;

        AutoBidEntry entry = candidate.get();
        double newBidAmount = currentPrice + entry.increment();

        // Giới hạn không vượt maxBid
        if (newBidAmount > entry.maxBid()) {
            newBidAmount = entry.maxBid();
        }

        // Double-check sau khi clamp
        if (newBidAmount <= currentPrice) return;

        // Lưu bid trực tiếp vào DB (không qua queue)
        try {
            BidTransactions db = new BidTransactions();
            UserStore userStore = new UserStore();

            User bidUser = userStore.getUser(entry.userId());
            Item dummyItem = ItemFactory.createItem(ItemType.Art, "auction-item", 0, "");
            db.saveBid(auctionId, new BidTransaction(bidUser, dummyItem, newBidAmount));

            System.out.printf("[AutoBid] Triggered | user=%s amount=%.0f auction=%s%n",
                    entry.userId(), newBidAmount, auctionId);


        } catch (Exception e) {
            System.err.println("[AutoBidEngine] triggerSync error: " + e.getMessage());
            return;
        }
    }

    // ── Helper ────────────────────────────────────────────────────
    private Optional<AutoBidEntry> resolveBestCandidate(
            List<AutoBidEntry> entries,
            double currentPrice,
            String currentWinnerId) {

        return entries.stream()
                .filter(e -> !e.userId().equals(currentWinnerId))   // không tự bid lại chính mình
                .filter(e -> e.maxBid() > currentPrice)             // còn đủ tiền
                .min(Comparator.comparingLong(AutoBidEntry::registeredAt)); // đăng ký sớm → ưu tiên
    }
}
