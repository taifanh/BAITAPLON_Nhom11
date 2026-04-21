package controllers.Server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionRoom {

    private static final AuctionRoom INSTANCE = new AuctionRoom();

    public static AuctionRoom getInstance() { return INSTANCE; }

    private AuctionRoom() {}

    // --- Danh sách tất cả client đang kết nối ---
    // Tương tự List<Consumer<String>> subscribers bên MessageBus
    // nhưng ở đây subscriber là ClientHandler (giữ socket thật)
    private final Set<ClientHandler> observers = ConcurrentHashMap.newKeySet();

    // --- Biết client nào đang xem phiên nào ---
    // key: clientHandler, value: auctionId họ đang xem
    private final Map<ClientHandler, String> watchingMap = new ConcurrentHashMap<>();

    // =========================================================
    // ĐĂNG KÝ / HỦY ĐĂNG KÝ
    // Tương tự subscribe/unsubscribe bên MessageBus
    // Gọi từ ClientHandler.run() khi client connect/disconnect
    // =========================================================

    public void register(ClientHandler handler) {
        observers.add(handler);
        System.out.println("[AuctionRoom] Client connected. Online: " + observers.size());
    }

    public void unregister(ClientHandler handler) {
        observers.remove(handler);
        watchingMap.remove(handler);
        System.out.println("[AuctionRoom] Client disconnected. Online: " + observers.size());
    }

    // Client báo đang xem phiên nào (gọi khi mở màn hình chi tiết)
    public void watch(ClientHandler handler, String auctionId) {
        watchingMap.put(handler, auctionId);
    }

    // Client rời khỏi màn hình chi tiết
    public void unwatch(ClientHandler handler) {
        watchingMap.remove(handler);
    }

    // =========================================================
    // BROADCAST
    // Tương tự dispatch() bên MessageBus
    // nhưng thay vì gọi consumer.accept(json)
    // thì gọi handler.send(json) — ghi thẳng xuống socket
    // =========================================================

    // Gửi đến TẤT CẢ client đang kết nối
    // Dùng cho: thông báo hệ thống, phiên mới được tạo
    public void broadcast(String json) {
        for (ClientHandler handler : observers) {
            handler.send(json);
        }
    }

    // Gửi đến những client đang xem 1 phiên cụ thể
    // Dùng cho: bid mới, cập nhật giá, đóng phiên
    public void broadcastToSession(String auctionId, String json) {
        for (ClientHandler handler : observers) {
            String watching = watchingMap.get(handler);
            if (auctionId.equals(watching)) {
                handler.send(json);
            }
        }
    }

    // Gửi đến tất cả TRỪ người gửi
    // Dùng khi muốn tránh echo lại cho chính client đặt bid
    public void broadcastExcept(String json, ClientHandler sender) {
        for (ClientHandler handler : observers) {
            if (handler != sender) {
                handler.send(json);
            }
        }
    }

    // Gửi riêng cho 1 client — không broadcast
    // Dùng cho: phản hồi lỗi, dữ liệu cá nhân
    public void sendTo(ClientHandler handler, String json) {
        handler.send(json);
    }

    // Query
    public int getOnlineCount() { return observers.size(); }
}
