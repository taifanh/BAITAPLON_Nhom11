package controllers.Server;

import Database.UserStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Extra.messages.Depositpayload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;                        // field — dùng lâu dài
    private final ObjectMapper mapper = new ObjectMapper();
    private String watchingAuctionId = null;        // client đang xem phiên nào

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // AuctionRoom gọi hàm này để push tin xuống client
    public void send(String json) {
        if (out != null && !socket.isClosed()) {
            out.println(json);
        }
    }

    public String getWatchingAuctionId() { return watchingAuctionId; }

    @Override
    public void run() {
        // Đăng ký vào AuctionRoom ngay khi connect
        AuctionRoom.getInstance().register(this);

        // Khởi tạo in/out NGOÀI try-with-resources
        // để out tồn tại suốt vòng đời ClientHandler
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            // Vòng lặp liên tục — tương tự while trong ServerConnection
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }

        } catch (IOException e) {
            System.out.println("[ClientHandler] Mất kết nối: " + e.getMessage());
        } finally {
            // Dù lỗi hay client tự ngắt đều unregister
            AuctionRoom.getInstance().unregister(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // =========================================================
    // XỬ LÝ TIN NHẮN TỪ CLIENT
    // =========================================================
    private void handleMessage(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            String type = resolveMessageType(node);

            switch (type) {

                case "WATCH_AUCTION" -> {
                    // Client mở màn hình chi tiết phiên
                    watchingAuctionId = node.get("auctionId").asText();
                    AuctionRoom.getInstance().watch(this, watchingAuctionId);
                    send(okJson("Đang theo dõi phiên: " + watchingAuctionId));
                }

                case "UNWATCH_AUCTION" -> {
                    AuctionRoom.getInstance().unwatch(this);
                    watchingAuctionId = null;
                }

                case "PLACE_BID" -> {
                    String auctionId = node.get("auctionId").asText();
                    /*Bid bid = mapper.treeToValue(node, Bid.class);

                    try {
                        // Manager xử lý nghiệp vụ, trả về JSON kết quả
                        String resultJson = AuctionManager.getInstance().placeBid(auctionId, bid);
                        // Room broadcast đến tất cả client đang xem phiên này
                        AuctionRoom.getInstance().broadcastToSession(auctionId, resultJson);

                    } catch (InvalidBidException | AuctionClosedException e) {
                        // Chỉ trả lỗi về client này, không broadcast
                        send(errorJson(e.getMessage()));
                    } */
                }

                case "GET_AUCTIONS" -> {
                    // TODO: lấy danh sách phiên từ AuctionManager, gửi riêng cho client này
                    send("{\"type\":\"AUCTION_LIST\",\"data\":[]}");
                }
                case "DEPOSIT" -> {
                    String userId = node.get("Id_user").asText();
                    String payloadJson = node.get("payloadJson").asText();

                    Depositpayload payload = mapper.readValue(payloadJson, Depositpayload.class);

                    UserStore userStore = new UserStore();
                    userStore.update_balance(payload.getAmount(), userId);
                    send(okJson("Deposit success"));

                }

                default -> System.out.println("[ClientHandler] Unknown type: " + type);
            }

        } catch (Exception e) {
            e.printStackTrace();
            send(errorJson("Lỗi xử lý yêu cầu"));
        }
    }

    // =========================================================
    // HELPER
    // =========================================================
    private String errorJson(String message) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "ERROR");
        node.put("message", message);
        return node.toString();
    }

    private String okJson(String message) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "OK");
        node.put("message", message);
        return node.toString();
    }

    private String resolveMessageType(JsonNode node) {
        String messageType = node.path("messageType").asText("");
        if (!messageType.isBlank()) {
            return messageType;
        }
        return node.path("type").asText("");
    }
}
