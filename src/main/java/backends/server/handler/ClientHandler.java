package backends.server.handler;

import backends.common.messages.Common.MessageType;
import backends.server.service.AvatarService;
import backends.server.service.AccountService;
import backends.server.service.AdminService;
import backends.server.service.AuctionProcessors;
import backends.server.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@FunctionalInterface
interface MessageProcessor {
    String process(ClientHandler handler, JsonNode node) throws Exception;
}

public class ClientHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public String watchingAuctionId = null;
    private String userId = null;
    private String role = null;

    private final Map<String, MessageProcessor> processors = new HashMap<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        registerProcessors();
    }

    private void registerProcessors() {
        processors.put(MessageType.AUCTION_ITEMS_RESPONSE.getValue(), AuctionProcessors::auctionItemsResponse);
        processors.put(MessageType.GET_BALANCE.getValue(), AccountService::getBalance);
        processors.put(MessageType.SIGN_IN.getValue(), UserService::signin);
        processors.put(MessageType.REGISTER_AUTO_BIDDING.getValue(), AuctionProcessors::registerAutoBid);
        processors.put(MessageType.CANCEL_AUTO_BIDDING.getValue(), AuctionProcessors::cancelAutoBid);
        processors.put(MessageType.SIGN_UP.getValue(), UserService::signup);
        processors.put(MessageType.FETCH_USER_REQUEST.getValue(), UserService::fetchUserRequest);
        processors.put(MessageType.FETCH_USER_BID_HISTORY.getValue(), UserService::fetchUserBidHistory);
        processors.put(MessageType.FETCH_INVENTORY.getValue(), AdminService::fetchInventory);
        processors.put(MessageType.FETCH_BID_HISTORY.getValue(), AdminService::fetchBidHistory);
        processors.put(MessageType.FETCH_AUCTION_STATUS.getValue(), AuctionProcessors::fetchAuctionStatus);
        processors.put(MessageType.FETCH_REQUESTS.getValue(), AdminService::fetchRequests);
        processors.put(MessageType.ADMIN_ACTION.getValue(), AdminService::adminAction);
        processors.put(MessageType.AUCTION_COMMAND.getValue(), AuctionProcessors::auctionCommand);
        processors.put(MessageType.WATCH_AUCTION.getValue(), AuctionProcessors::watchAuction);
        processors.put(MessageType.UNWATCH_AUCTION.getValue(), AuctionProcessors::unwatchAuction);
        processors.put(MessageType.PLACE_BID.getValue(), AuctionProcessors::placeBid);
        processors.put(MessageType.GET_AUCTIONS.getValue(), AuctionProcessors::getAuctions);
        processors.put(MessageType.DEPOSIT.getValue(), AccountService::deposit);
        processors.put(MessageType.ADD_ITEM.getValue(), UserService::addItem);
        processors.put(MessageType.CHANGE_INFO.getValue(), AccountService::changeInfo);
        processors.put(MessageType.GET_AVATAR.getValue(), AvatarService::getAvatar);
        processors.put(MessageType.SAVE_AVATAR.getValue(), AvatarService::saveAvatar);
        processors.put(MessageType.REMOVE_ITEM.getValue(), UserService::removeItem);
    }

    // AuctionRoom gọi hàm này để push tin xuống client
    public void send(String json) {
        if (out != null && !socket.isClosed()) {
            out.println(json);
        }
    }

    public String getWatchingAuctionId() { return watchingAuctionId; }

    public String getRole() { return role; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setRole(String role) { this.role = role; }

    @Override
    public void run() {
        // Đăng ký vào AuctionRoom ngay khi connect
        AuctionRoom.getInstance().register(this);

        // Khởi tạo in/out NGOÀI try-with-resources
        // để out tồn tại suốt vòng đời ClientHandler
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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

            MessageProcessor processor = processors.get(type);
            if (processor != null) {
                String response = processor.process(this, node);
                if (response != null) {
                    send(response);
                }
            } else {
                System.out.println("[ClientHandler] Unknown type: " + type);
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

    private String okJson(Double amount) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "BALANCE_OK");
        node.put("amount", amount);
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
