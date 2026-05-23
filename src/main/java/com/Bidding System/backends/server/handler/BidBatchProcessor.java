package backends.server.handler;

import backends.common.messages.MsgBid.ReceiveMaxBidder;
import backends.common.messages.MsgBid.ServerBidRespond;
import backends.common.models.accounts.User;
import backends.common.models.bidding.Auction;
import backends.common.models.bidding.BidTransaction;
import backends.common.models.core.Item;
import backends.common.models.items.ItemFactory;
import backends.common.models.items.ItemType;
import backends.server.database.BidTransactionDAO;
import backends.server.database.UserDAO;
import backends.server.service.AuctionService;
import com.google.gson.Gson;

public class BidBatchProcessor {

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static BidBatchProcessor instance;
    public static synchronized BidBatchProcessor getInstance() {
        if (instance == null) instance = new BidBatchProcessor();
        return instance;
    }
    private BidBatchProcessor() {}

    // ── Public API ─────────────────────────────────────────────────────────────

    public synchronized void submitBid(String userId, String auctionId, double amount) {
        try {
            BidTransactionDAO bidDAO  = new BidTransactionDAO();
            UserDAO userDAO = new UserDAO();
            Item dummyItem = ItemFactory.createItem(ItemType.Art, "auction-item", 0, "");

            // 1. Lấy giá cao nhất hiện tại
            ServerBidRespond currentMax = bidDAO.getMaxBidder(auctionId);
            double currentMaxAmount = (currentMax != null) ? currentMax.amount : 0;

            // 2. Kiểm tra bid hợp lệ
            if (amount <= currentMaxAmount) {
                // Bid không hợp lệ → vẫn broadcast giá hiện tại để client biết
                broadcastMaxBidder(auctionId, currentMax);
                return;
            }

            // 3. Lưu bid vào DB
            User bidUser = userDAO.getUser(userId);
            bidDAO.saveBid(auctionId, new BidTransaction(bidUser, dummyItem, amount));

            // 4. Kích hoạt auto-bid engine phản hồi trước
            AutoBidEngine.getInstance().resolveAuction(auctionId);

            // 5. Broadcast kết quả cuối cùng sau khi auto-bid đã xong
            ServerBidRespond newMax = bidDAO.getMaxBidder(auctionId);
            broadcastMaxBidder(auctionId, newMax);

            // 6. Kiểm tra anti-snipe
            Auction managedAuction = AuctionService.getManagedActiveAuctionByAuctionId(auctionId);
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

    // ── Private ────────────────────────────────────────────────────────────────
    private void broadcastMaxBidder(String auctionId, ServerBidRespond maxBidder) {
        if (maxBidder == null) return;
        maxBidder.auctionId = auctionId;
        String json = new Gson().toJson(new ReceiveMaxBidder(maxBidder));
        AuctionRoom.getInstance().broadcast(json);
    }
}