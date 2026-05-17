package backends.server.handler;

import backends.server.database.BidTransactions;
import backends.server.database.Inventory;
import backends.server.database.UserDAOImpl;
import backends.server.service.AuctionService;
import backends.common.messages.MsgAuction.AuctionResultMessage;
import backends.common.messages.MsgBid.ServerBidRespond;
import backends.common.messages.MsgAuction.StartAuctionMessage;
import backends.common.models.accounts.Admin;
import backends.common.models.accounts.User;
import backends.common.models.bidding.Auction;
import backends.common.models.core.Item;
import backends.common.messages.MsgAuction.AuctionStatusMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;

public class ServerAuctionManager {
    private static ServerAuctionManager instance;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Tạo 1 Admin giả lập đại diện cho Server để truyền vào hàm startAuction của bạn
    private final Admin serverAdmin = new Admin(
            "SERVER_001",
            "System Server",
            "server@system.com",
            "11111",
            "server"
    );

    private ServerAuctionManager() {
        // TUYỆT VỜI: Tận dụng luôn hàm khôi phục của bạn khi khởi động Server!
        try {
            AuctionService.restoreActiveAuctionsOnStartup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized ServerAuctionManager getInstance() {
        if (instance == null) {
            instance = new ServerAuctionManager();
        }
        return instance;
    }

    // Xử lý khi Admin gửi lệnh START
    public void startAuction(String itemId, int durationMinutes) {
        try {
            Inventory inventoryDB = new Inventory();
            Item item = inventoryDB.findById(itemId);
            if (item == null) {
                System.out.println("[Server] Loi: Khong tim thay item " + itemId);
                return;
            }

            // Gọi AuctionService của bạn (Nó sẽ tự lo Timer và set DB IN_PROGRESS)
            Auction auction = AuctionService.startAuction(serverAdmin, item, 0, durationMinutes, 0);
            // Báo cho tất cả Client/Admin trên mạng lưới biết
            AuctionStatusMessage statusMsg = new AuctionStatusMessage();
            statusMsg.status = "STARTED";
            statusMsg.itemId = itemId;
            statusMsg.auctionId = auction.getAuctionId();
            statusMsg.endTimeEpoch = System.currentTimeMillis() + (durationMinutes * 60000L);

            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(statusMsg));
            String sellerId = inventoryDB.getUserIdByItemId(auction.getItem().getId());
            StartAuctionMessage start_msg = new StartAuctionMessage(
                    statusMsg.endTimeEpoch,
                    auction.getItem().getName(),
                    sellerId,
                    auction.getAuctionId(),
                    auction.getItem().getPrices(),
                    item.getBidIncrement()
            );
            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(start_msg));
        } catch (Exception e) {
            System.err.println("[Server] Loi start auction: " + e.getMessage());
        }
    }

    // Xử lý khi Admin ép buộc END
    public void endAuction(String itemId) {
        try {
            Auction auction = AuctionService.getManagedActiveAuction(itemId);
            if (auction != null) {
                // Gọi AuctionService để End (Nó sẽ tự hủy Timer đang chạy dở, update DB sang SOLD/UNSOLD)
                AuctionService.endAuction(auction, LocalDateTime.now());
                broadcastEnd(itemId, auction);
            } else {
                // Fix lỗi Orphan (Có trong DB nhưng mất trong RAM)
                Inventory inventoryDB = new Inventory();
                inventoryDB.updateItemStatus(itemId, Inventory.STATUS_WAITING);
                broadcastEnd(itemId, auction);
            }
        } catch (Exception e) {
            System.err.println("[Server] Loi end auction: " + e.getMessage());
        }
    }

    // Hàm phụ để đẩy tin nhắn kết thúc (Dùng chung cho cả tự động và thủ công)
    public void broadcastEnd(String itemId, Auction auction) {
        try {
            if (auction == null) {
                AuctionStatusMessage statusMsg = new AuctionStatusMessage();
                statusMsg.status = "ENDED";
                statusMsg.itemId = itemId;
                statusMsg.endTimeEpoch = 0;
                AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(statusMsg));
                return;
            }

            String auctionId = auction.getAuctionId();

            if (auctionId != null) {
                BidBatchProcessor.getInstance().flushAuction(auctionId);
            }

            AuctionStatusMessage statusMsg = new AuctionStatusMessage();
            statusMsg.status = "ENDED";
            statusMsg.itemId = itemId;
            statusMsg.endTimeEpoch = 0;
            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(statusMsg));
            //broadcast kết quả của phien đấu giá
            AuctionResultMessage result = new AuctionResultMessage();
            result.itemId = itemId;
            result.itemName = auction.getItem().getName();
            BidTransactions bidDb = new BidTransactions();
            ServerBidRespond maxBidder = bidDb.getMaxBidder(auction.getAuctionId());
            if (maxBidder != null && maxBidder.userId != null) {
                result.hasBidder = true;
                result.winnerId = maxBidder.userId;
                result.winningAmount = maxBidder.amount;

                UserDAOImpl userDAOImpl = new UserDAOImpl();
                User winner = userDAOImpl.getUser(maxBidder.userId);
                result.winnerName = (winner != null) ? winner.getName() : maxBidder.userId;
                userDAOImpl.update_balance(-result.winningAmount, result.winnerId);
            } else {
                result.hasBidder = false;
                result.winnerName = "Không có người thắng";
            }
            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
