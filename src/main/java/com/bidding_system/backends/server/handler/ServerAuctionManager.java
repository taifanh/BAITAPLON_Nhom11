package com.bidding_system.backends.server.handler;

import com.bidding_system.backends.server.database.BidTransactionDAO;
import com.bidding_system.backends.server.database.InventoryDAO;
import com.bidding_system.backends.server.database.UserDAO;
import com.bidding_system.backends.server.policy.IncrementPolicy;
import com.bidding_system.backends.server.service.AuctionService;
import com.bidding_system.backends.common.messages.MsgAuction.AuctionResultMessage;
import com.bidding_system.backends.common.messages.MsgBid.ServerBidRespond;
import com.bidding_system.backends.common.messages.MsgAuction.StartAuctionMessage;
import com.bidding_system.backends.common.models.accounts.Admin;
import com.bidding_system.backends.common.models.accounts.User;
import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.common.models.core.Item;
import com.bidding_system.backends.common.messages.MsgAuction.AuctionStatusMessage;
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
            InventoryDAO inventoryDAODB = new InventoryDAO();
            Item item = inventoryDAODB.findById(itemId);
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
            statusMsg.increment = 0;
            statusMsg.auctionId = auction.getAuctionId();
            statusMsg.endTimeEpoch = System.currentTimeMillis() + (durationMinutes * 60000L);

            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(statusMsg));
            String sellerId = inventoryDAODB.getUserIdByItemId(auction.getItem().getId());
            StartAuctionMessage start_msg = new StartAuctionMessage(
                    statusMsg.endTimeEpoch,
                    auction.getItem().getName(),
                    sellerId,
                    auction.getAuctionId(),
                    auction.getItem().getPrices(),
                    0
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
                InventoryDAO inventoryDAODB = new InventoryDAO();
                inventoryDAODB.updateItemStatus(itemId, InventoryDAO.STATUS_WAITING);
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
            AuctionStatusMessage statusMsg = new AuctionStatusMessage();
            statusMsg.status = "ENDED";
            statusMsg.itemId = itemId;
            statusMsg.endTimeEpoch = 0;
            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(statusMsg));
            //broadcast kết quả của phien đấu giá
            AuctionResultMessage result = new AuctionResultMessage();
            result.itemId = itemId;
            result.itemName = auction.getItem().getName();
            BidTransactionDAO bidDb = new BidTransactionDAO();
            ServerBidRespond maxBidder = bidDb.getMaxBidder(auction.getAuctionId());
            if (maxBidder != null && maxBidder.userId != null) {
                result.hasBidder = true;
                result.winnerId = maxBidder.userId;
                result.winningAmount = maxBidder.amount;

                UserDAO userDAO = new UserDAO();
                User winner = userDAO.getUser(maxBidder.userId);
                result.winnerName = (winner != null) ? winner.getName() : maxBidder.userId;
                userDAO.update_balance(-result.winningAmount, result.winnerId);
            } else {
                result.hasBidder = false;
                result.winnerName = "Không có người thắng";
            }
            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // thông báo cho user đang xem phiên hệ thống cập nhật đếm giờ mới
    public void broadcastAuctionExtended(Auction auction) {
        try {
            AuctionStatusMessage statusMsg = new AuctionStatusMessage();
            statusMsg.status = "STARTED";
            statusMsg.itemId = auction.getItem().getId();
            statusMsg.auctionId = auction.getAuctionId();

            long remainingMillis = java.time.Duration.between(
                    LocalDateTime.now(),
                    auction.getEndAt()
            ).toMillis();
            statusMsg.endTimeEpoch = System.currentTimeMillis() + Math.max(remainingMillis, 0);

            InventoryDAO inventoryDB = new InventoryDAO();
            statusMsg.sellerId = inventoryDB.getUserIdByItemId(auction.getItem().getId());
            statusMsg.startingPrice = String.valueOf(auction.getItem().getPrices());

            BidTransactionDAO bidDb = new BidTransactionDAO();
            ServerBidRespond maxBidder = bidDb.getMaxBidder(auction.getAuctionId());
            if (maxBidder != null && maxBidder.userId != null) {
                statusMsg.maxBidderAmount = maxBidder.amount;

                UserDAO userStore = new UserDAO();
                User winner = userStore.getUser(maxBidder.userId);
                statusMsg.maxBidderName = (winner != null) ? winner.getName() : maxBidder.userId;
            }

            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(statusMsg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
