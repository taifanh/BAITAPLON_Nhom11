package controllers.Server;

import Database.Inventory;
import controllers.AuctionService;
import models.accounts.Admin;
import models.bidding.Auction;
import models.core.Item;
import models.Extra.messages.AuctionStatusMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public class ServerAuctionManager {
    private static ServerAuctionManager instance;
    private final ObjectMapper mapper = new ObjectMapper();

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
                broadcastEnd(itemId);
            } else {
                // Fix lỗi Orphan (Có trong DB nhưng mất trong RAM)
                Inventory inventoryDB = new Inventory();
                inventoryDB.updateItemStatus(itemId, Inventory.STATUS_WAITING);
                broadcastEnd(itemId);
            }
        } catch (Exception e) {
            System.err.println("[Server] Loi end auction: " + e.getMessage());
        }
    }

    // Hàm phụ để đẩy tin nhắn kết thúc (Dùng chung cho cả tự động và thủ công)
    public void broadcastEnd(String itemId) {
        try {
            AuctionStatusMessage statusMsg = new AuctionStatusMessage();
            statusMsg.status = "ENDED";
            statusMsg.itemId = itemId;
            statusMsg.endTimeEpoch = 0;
            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(statusMsg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}