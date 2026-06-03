package backends.server.service;

import backends.common.messages.MsgData.RequestStatusUpdateMessage;
import backends.server.database.InventoryDAO;
import backends.server.database.MyRequestDAO;
import backends.server.handler.AuctionRoom;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class RequestStatusNotifier {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RequestStatusNotifier() {
    }

    // Gửi 1 event trạng thái chung cho seller.
    // Các service chỉ cần update DB xong là gọi helper này, tránh lặp logic broadcast.
    public static void notifyByRequestId(String requestId, String itemId, String status) {
        if (requestId == null || requestId.isBlank() || status == null || status.isBlank()) {
            return;
        }

        try {
            String userId = resolveUserId(requestId, itemId);
            if (userId == null || userId.isBlank()) {
                return;
            }

            RequestStatusUpdateMessage message = new RequestStatusUpdateMessage(requestId, status, itemId);
            message.sellerId = userId;
            AuctionRoom.sendToUser(userId, MAPPER.writeValueAsString(message));
        } catch (Exception e) {
            System.err.println("[RequestStatusNotifier] Khong the gui cap nhat trang thai: " + e.getMessage());
        }
    }

    // Ưu tiên lấy userId từ my_request vì đây là nguồn gắn với seller-side list.
    // Nếu không có thì fallback sang inventory để vẫn gửi được event.
    private static String resolveUserId(String requestId, String itemId) throws Exception {
        MyRequestDAO myRequestDAO = new MyRequestDAO();
        MyRequestDAO.RequestRecord request = myRequestDAO.findByRequestId(requestId);
        if (request != null && request.userId() != null && !request.userId().isBlank()) {
            return request.userId();
        }

        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        InventoryDAO inventoryDAO = new InventoryDAO();
        return inventoryDAO.getUserIdByItemId(itemId);
    }
}
