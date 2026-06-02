package backends.server.handler;

import backends.common.models.core.Account;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionRoom {

    // --- Singleton, cùng kiểu eager như AuctionManager ---
    private static final AuctionRoom INSTANCE = new AuctionRoom();

    public static AuctionRoom getInstance() { return INSTANCE; }

    private AuctionRoom() {}

    // --- Danh sách tất cả client đang kết nối ---
    // Tương tự List<Consumer<String>> subscribers bên MessageBus
    // nhưng ở đây subscriber là ClientHandler (giữ socket thật)
    private final Set<ClientHandler> observers = ConcurrentHashMap.newKeySet();
    // dùng cho gửi tin cho 1 số client nhất định
    public final Map<String , ClientHandler> connectors = new ConcurrentHashMap<>();
    // --- Biết client nào đang xem phiên nào ---
    // key: clientHandler, value: auctionId họ đang xem
    private final Map<ClientHandler, String> watchingMap = new ConcurrentHashMap<>();


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

    // Gửi đến TẤT CẢ client đang kết nối
    // Dùng cho: thông báo hệ thống, phiên mới được tạo
    public void broadcast(String json) {
        for (ClientHandler handler : observers) {
            handler.send(json);
        }
    }
    public static void sendToAdmin(String json) {
        for (ClientHandler handler : AuctionRoom.getInstance().observers) {
            if (handler.getRole() != null && handler.getRole().equalsIgnoreCase(Account.ADMIN)) {
                handler.send(json);
            }
        }
    }
    public static void sendToUser(String userId,String json) {
        ClientHandler handler = AuctionRoom.getInstance().connectors.get(userId);
        if (handler != null) {
            handler.send(json);
        }
    }

    public void disconnectAll() {
        for (ClientHandler handler : observers) {
            try {
                handler.close();
            } catch (Exception ignored) {
            }
        }

        observers.clear();
        connectors.clear();
        watchingMap.clear();
    }

}
