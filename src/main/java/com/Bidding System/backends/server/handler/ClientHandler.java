package backends.server.handler;

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
        processors.put("AUCTION_ITEMS_RESPONSE", AuctionProcessors::auctionItemsResponse);
        processors.put("GET_BALANCE", AccountService::getBalance);
        processors.put("signin", UserService::signin);
        processors.put("signup", UserService::signup);
        processors.put("FETCH_INVENTORY", AdminService::fetchInventory);
        processors.put("FETCH_AUCTION_STATUS", AuctionProcessors::fetchAuctionStatus);
        processors.put("FETCH_REQUESTS", AdminService::fetchRequests);
        processors.put("ADMIN_ACTION", AdminService::adminAction);
        processors.put("AUCTION_COMMAND", AuctionProcessors::auctionCommand);
        processors.put("WATCH_AUCTION", AuctionProcessors::watchAuction);
        processors.put("UNWATCH_AUCTION", AuctionProcessors::unwatchAuction);
        processors.put("PLACE_BID", AuctionProcessors::placeBid);
        processors.put("GET_AUCTIONS", AuctionProcessors::getAuctions);
        processors.put("DEPOSIT", AccountService::deposit);
        processors.put("additem", UserService::addItem);
        processors.put("change_info", AccountService::changeInfo);
        processors.put("removeitem", UserService::removeItem);
    }

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
        AuctionRoom.getInstance().register(this);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }
        } catch (IOException e) {
            System.out.println("[ClientHandler] Mất kết nối: " + e.getMessage());
        } finally {
            AuctionRoom.getInstance().unregister(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

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

    private String errorJson(String message) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "ERROR");
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
