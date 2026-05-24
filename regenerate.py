
import os

os.makedirs("src/main/java/com/Bidding System/backends/server/service", exist_ok=True)
os.makedirs("src/main/java/com/Bidding System/backends/server/handler", exist_ok=True)

client_handler = """package backends.server.handler;

import com.bidding_system.backends.server.service.AccountService;
import com.bidding_system.backends.server.service.AdminService;
import com.bidding_system.backends.server.service.AuctionProcessors;
import com.bidding_system.backends.server.service.UserService;
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
"""

account_service = """package backends.server.service;

import com.bidding_system.backends.common.messages.Common.Change_infopayload;
import com.bidding_system.backends.common.messages.Common.Depositpayload;
import com.bidding_system.backends.common.messages.Common.Message;
import com.bidding_system.backends.server.database.UserDAO;
import com.bidding_system.backends.server.handler.ClientHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;

public class AccountService {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final Gson gson = new Gson();

    public static String getBalance(ClientHandler handler, JsonNode node) throws Exception {
        Message msg = mapper.treeToValue(node, Message.class);
        UserDAOImpl userDAO = new UserDAOImpl();
        double currentBalance = userDAO.get_balance(msg.Id_user);
        
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("type", "BALANCE_OK");
        responseNode.put("amount", currentBalance);
        return responseNode.toString();
    }

    public static String deposit(ClientHandler handler, JsonNode node) throws Exception {
        String userId = node.get("Id_user").asText();
        String payloadJson = node.get("payloadJson").asText();

        Depositpayload payload = mapper.readValue(payloadJson, Depositpayload.class);
        System.out.println("[Server] DEPOSIT received | userId=" + userId + " | amount=" + payload.getAmount());

        UserDAOImpl userDAO = new UserDAOImpl();
        userDAO.update_balance(payload.getAmount(), userId);
        payload.setAmount(userDAO.get_balance(userId));
        
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("type", "deposit_OK");
        responseNode.put("payloadJson", gson.toJson(payload));

        return responseNode.toString();
    }

    public static String changeInfo(ClientHandler handler, JsonNode node) throws Exception {
        String payloadJson = node.get("payloadJson").asText();
        Change_infopayload payload = mapper.readValue(payloadJson, Change_infopayload.class);

        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("type", "change_info_OK");
        responseNode.put("payloadJson", gson.toJson(payload));

        return responseNode.toString();
    }
}
"""

admin_service = """package backends.server.service;

import com.bidding_system.backends.common.messages.Common.Createitempayload;
import com.bidding_system.backends.common.messages.MsgAuction.AdminActionCommand;
import com.bidding_system.backends.common.messages.MsgData.InventoryDataResponse;
import com.bidding_system.backends.common.messages.MsgData.RequestListDataResponse;
import com.bidding_system.backends.common.models.accounts.Admin;
import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.common.models.core.Item;
import com.bidding_system.backends.common.models.items.ItemFactory;
import com.bidding_system.backends.common.models.items.ItemType;
import com.bidding_system.backends.server.database.InventoryDAO;
import com.bidding_system.backends.server.database.RequestLogDAO;
import com.bidding_system.backends.server.database.MyRequestDAO;
import com.bidding_system.backends.server.handler.AuctionRoom;
import com.bidding_system.backends.server.handler.ClientHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;

import java.io.IOException;

public final class AdminService {
    private static final String ADD_ITEM_REQUEST_TYPE = "additem";
    private static final Gson GSON = new Gson();
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AdminService() {
    }

    public static Auction startAuction(Admin admin, int hours, int minutes, int seconds) throws IOException {
        return AuctionService.startAuction(admin, hours, minutes, seconds);
    }

    public static Auction startAuction(Admin admin, Item item, int hours, int minutes, int seconds) throws IOException {
        return AuctionService.startAuction(admin, item, hours, minutes, seconds);
    }

    public static String fetchInventory(ClientHandler handler, JsonNode node) throws Exception {
        Inventory inventoryDAODB = new Inventory();
        InventoryDataResponse response = new InventoryDataResponse();

        response.waitingItems = inventoryDAODB.getItemsByStatus(Inventory.STATUS_WAITING);
        response.scheduledItems = inventoryDAODB.getItemsByStatus(Inventory.STATUS_SCHEDULED);
        response.inProgressItems = inventoryDAODB.getItemsByStatus(Inventory.STATUS_IN_PROGRESS);

        return mapper.writeValueAsString(response);
    }

    public static String fetchRequests(ClientHandler handler, JsonNode node) throws Exception {
        RequestLog requestLogDAODB = new RequestLog();
        RequestListDataResponse response = new RequestListDataResponse();

        response.requests = requestLogDAODB.getRequestsByType("additem");

        return mapper.writeValueAsString(response);
    }

    public static String adminAction(ClientHandler handler, JsonNode node) throws Exception {
        AdminActionCommand cmd = mapper.treeToValue(node, AdminActionCommand.class);
        Inventory inventoryDAODB = new Inventory();
        RequestLog requestLogDAODB = new RequestLog();

        if ("SCHEDULE_ITEM".equals(cmd.action)) {
            inventoryDAODB.updateItemStatus(cmd.targetId, Inventory.STATUS_SCHEDULED);

            ObjectNode ack = mapper.createObjectNode();
            ack.put("type", "ACTION_SUCCESS");
            handler.send(ack.toString());

            InventoryDataResponse inventoryResponse = new InventoryDataResponse();
            inventoryResponse.waitingItems = inventoryDAODB.getItemsByStatus(Inventory.STATUS_WAITING);
            inventoryResponse.scheduledItems = inventoryDAODB.getItemsByStatus(Inventory.STATUS_SCHEDULED);
            inventoryResponse.inProgressItems = inventoryDAODB.getItemsByStatus(Inventory.STATUS_IN_PROGRESS);

            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(inventoryResponse));
            return null; // Already sent ack
        } else if ("REJECT_REQUEST".equals(cmd.action)) {
            requestLogDAODB.updateRequestStatus(cmd.targetId, RequestLog.STATUS_REJECTED);

            ObjectNode ack = mapper.createObjectNode();
            ack.put("type", "ACTION_SUCCESS");
            return ack.toString();
        } else if ("ACCEPT_REQUEST".equals(cmd.action)) {
            RequestLog.RequestRecord request = requestLogDAODB.findByRequestId(cmd.targetId);
            if (request != null) {
                Createitempayload payload = GSON.fromJson(request.requestInfo(), Createitempayload.class);
                ItemType itemType = ItemType.valueOf(payload.getItemType());

                Item item = ItemFactory.createItem(
                        itemType,
                        payload.getItem_name(),
                        payload.getBasePrice(),
                        payload.getItemInfo(),
                        payload.getBidIncrement()
                );

                inventoryDAODB.saveItem(item, request.userId(), request.id());
                requestLogDAODB.updateRequestStatus(cmd.targetId, RequestLog.STATUS_WAITING);
                
                ObjectNode response = mapper.createObjectNode();
                response.put("type", "ACCEPTED_SUCCESS");
                String targetUserId = (cmd.userId != null && !cmd.userId.isBlank())
                        ? cmd.userId
                        : request.userId();
                response.put("user_id", targetUserId);
                response.put("request_id", cmd.targetId);
                response.put("status", RequestLog.STATUS_WAITING);

                ClientHandler targetHandler = AuctionRoom.getInstance().connectors.get(targetUserId);
                if (targetHandler != null) {
                    targetHandler.send(String.valueOf(response));
                }

                ObjectNode ack = mapper.createObjectNode();
                ack.put("type", "ACTION_SUCCESS");
                return ack.toString();
            }
        }
        return null;
    }
}
"""

auction_processors = """package backends.server.service;

import com.bidding_system.backends.common.messages.MsgAuction.AuctionCommandMessage;
import com.bidding_system.backends.common.messages.MsgAuction.AuctionStatusMessage;
import com.bidding_system.backends.common.messages.MsgBid.ClientSendBid;
import com.bidding_system.backends.common.messages.MsgBid.ServerBidRespond;
import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.server.database.BidTransactionDAO;
import com.bidding_system.backends.server.database.InventoryDAO;
import com.bidding_system.backends.server.database.UserDAO;
import com.bidding_system.backends.server.handler.AuctionRoom;
import com.bidding_system.backends.server.handler.BidProcessor;
import com.bidding_system.backends.server.handler.ClientHandler;
import com.bidding_system.backends.server.handler.ServerAuctionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class AuctionProcessors {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static UserDAOImpl userDAO = new UserDAOImpl();

    public static String auctionItemsResponse(ClientHandler handler, JsonNode node) throws Exception {
        AuctionRoom.getInstance().broadcast(node.toString());
        return null;
    }

    public static String fetchAuctionStatus(ClientHandler handler, JsonNode node) throws Exception {
        String itemId = node.get("itemId").asText();
        Auction managedAuction = AuctionService.getManagedActiveAuction(itemId);
        java.time.Duration remaining = AuctionService.getDuration(itemId);
        AuctionStatusMessage statusMsg = new AuctionStatusMessage();
        statusMsg.itemId = itemId;
        statusMsg.auctionId = (managedAuction != null) ? managedAuction.getAuctionId() : "";
        boolean isRunning = !remaining.isZero() && !remaining.isNegative() && managedAuction != null;
        if (isRunning) {
            statusMsg.status = "STARTED";
            statusMsg.endTimeEpoch = System.currentTimeMillis() + remaining.toMillis();
            try {
                Inventory inventoryDAODB = new Inventory();
                statusMsg.sellerId = inventoryDAODB.getUserIdByItemId(itemId);
            } catch (Exception e) {
                statusMsg.sellerId = "";
            }
            BidTransactions bidDb = new BidTransactions();
            ServerBidRespond maxBidder = bidDb.getMaxBidder(managedAuction.getAuctionId());
            if (maxBidder != null && maxBidder.userId != null) {
                statusMsg.maxBidderAmount = String.valueOf(maxBidder.amount);
                String name = userDAO.getNameById(maxBidder.userId);
                statusMsg.maxBidderName = (name != null) ? name : "";
            }
        } else if (managedAuction == null) {
            statusMsg.status = "NOT_STARTED";
            statusMsg.endTimeEpoch = 0;
        } else {
            statusMsg.status = "ENDED";
            statusMsg.endTimeEpoch = 0;
        }
        return mapper.writeValueAsString(statusMsg);
    }

    public static String auctionCommand(ClientHandler handler, JsonNode node) throws Exception {
        AuctionCommandMessage cmd = mapper.treeToValue(node, AuctionCommandMessage.class);

        if ("START".equals(cmd.command)) {
            System.out.println("[Server] Nhan lenh START cho item: " + cmd.itemId);
            ServerAuctionManager.getInstance().startAuction(cmd.itemId, cmd.durationMinutes);

        } else if ("END".equals(cmd.command)) {
            System.out.println("[Server] Nhan lenh END ep buoc cho item: " + cmd.itemId);
            ServerAuctionManager.getInstance().endAuction(cmd.itemId);
        }
        return null;
    }

    public static String watchAuction(ClientHandler handler, JsonNode node) throws Exception {
        String watchingAuctionId = node.get("auctionId").asText();
        handler.watchingAuctionId = watchingAuctionId;
        AuctionRoom.getInstance().watch(handler, watchingAuctionId);
        return null;
    }

    public static String unwatchAuction(ClientHandler handler, JsonNode node) throws Exception {
        AuctionRoom.getInstance().unwatch(handler);
        handler.watchingAuctionId = null;
        return null;
    }

    public static String placeBid(ClientHandler handler, JsonNode node) throws Exception {
        ClientSendBid info = mapper.treeToValue(node, ClientSendBid.class);

        String auctionId = (info.auctionId != null && !info.auctionId.isBlank())
                ? info.auctionId
                : handler.watchingAuctionId;

        if (auctionId == null || auctionId.isBlank()) {
            ObjectNode error = mapper.createObjectNode();
            error.put("type", "ERROR");
            error.put("message", "Không xác định được phiên đấu giá");
            return error.toString();
        }

        BidBatchProcessor.getInstance().submitBid(info.id, auctionId, info.amount);

        ObjectNode ack = mapper.createObjectNode();
        ack.put("type", "BID_QUEUED");
        ack.put("auctionId", auctionId);
        ack.put("amount", info.amount);
        return ack.toString();
    }

    public static String getAuctions(ClientHandler handler, JsonNode node) throws Exception {
        return "{\\"type\\":\\"AUCTION_LIST\\",\\"data\\":[]}";
    }
}
"""

user_service = """package backends.server.service;

import com.bidding_system.backends.common.messages.Common.Createitempayload;
import com.bidding_system.backends.common.messages.Common.Message;
import com.bidding_system.backends.common.messages.Common.RemoveRequestpayload;
import com.bidding_system.backends.common.messages.Common.SigninPayload;
import com.bidding_system.backends.common.messages.Common.SigninResponsePayload;
import com.bidding_system.backends.common.messages.Common.SignupPayload;
import com.bidding_system.backends.common.messages.MsgData.InventoryDataResponse;
import com.bidding_system.backends.common.messages.MsgData.RequestListDataResponse;
import com.bidding_system.backends.common.models.accounts.User;
import com.bidding_system.backends.common.models.core.Account;
import com.bidding_system.backends.server.database.InventoryDAO;
import com.bidding_system.backends.server.database.MyRequestDAO;
import com.bidding_system.backends.server.database.RequestLogDAO;
import com.bidding_system.backends.server.database.UserDAO;
import com.bidding_system.backends.server.handler.AuctionRoom;
import com.bidding_system.backends.server.handler.ClientHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Optional;

public final class UserService {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static UserDAOImpl userDAO = new UserDAOImpl();
    public static Gson gson = new Gson();

    public static String signin(ClientHandler clientHandler, JsonNode node) throws IOException {
        SigninPayload payload = mapper.readValue(node.get("payloadJson").asText(), SigninPayload.class);
        Optional<Account> accountOptional =
                userDAO.authenticate(payload.getPhoneNumber(), payload.getPassword());

        if (accountOptional.isEmpty()) {
            ObjectNode fail = mapper.createObjectNode();
            fail.put("type", "SIGNIN_FAIL");
            return fail.toString();
        }

        Account account = accountOptional.get();
        clientHandler.setUserId(account.getId());
        clientHandler.setRole(account.getRole());
        AuctionRoom.getInstance().connectors.put(clientHandler.getUserId(), clientHandler);

        double balance = account instanceof User user ? user.getBalance() : 0.0;

        SigninResponsePayload responsePayload = new SigninResponsePayload(
                account.getId(),
                account.getName(),
                account.getEmail(),
                account.getPhoneNumber(),
                account.getPassword(),
                account.getRole(),
                balance
        );

        ObjectNode ok = mapper.createObjectNode();
        ok.put("type", "SIGNIN_OK");
        ok.put("payloadJson", gson.toJson(responsePayload));
        return ok.toString();
    }

    public static String signup(ClientHandler clientHandler, JsonNode node) throws IOException {
        SignupPayload payload = mapper.readValue(node.get("payloadJson").asText(), SignupPayload.class);
        UserDAOImpl userDAO = new UserDAOImpl();
        if (userDAO.phoneNumberExists(payload.getPhoneNumber())) {
            ObjectNode fail = mapper.createObjectNode();
            fail.put("type", "SIGNUP_FAIL");
            return fail.toString();
        }
        userDAO.saveUser(new User(payload.getName(),  payload.getEmail(), payload.getPhoneNumber(), payload.getPassword()));

        ObjectNode success = mapper.createObjectNode();
        success.put("type", "SIGNUP_OK");
        return success.toString();
    }

    public static String addItem(ClientHandler handler, JsonNode node) throws Exception {
        String userId = node.get("Id_user").asText();
        String payloadJson = node.get("payloadJson").asText();

        Message msg = new Message();
        msg.Id_user = userId;
        msg.payloadJson = payloadJson;
        msg.messageType = "additem";

        Createitempayload payload = mapper.readValue(payloadJson, Createitempayload.class);
        
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("type", "add_item_OK");
        responseNode.put("payloadJson", gson.toJson(payload));

        String requestId = RequestLog.save_request(msg);
        responseNode.put("request_id", requestId);
        
        handler.send(responseNode.toString());
        AuctionRoom.sendadmin(responseNode.toString());
        return null;
    }

    public static String removeItem(ClientHandler handler, JsonNode node) throws Exception {
        String payloadJson = node.get("payloadJson").asText();

        RemoveRequestpayload payload = mapper.readValue(payloadJson, RemoveRequestpayload.class);
        String status_item = payload.getStatus();
        RequestLog requestlog = new RequestLog();
        Inventory inventoryDAODB = new Inventory();

        if (MyRequest.STATUS_IN_PROGRESS.equals(status_item)
                || MyRequest.STATUS_SCHEDULED.equals(status_item)) {
            ObjectNode response = mapper.createObjectNode();
            response.put("type", "remove_item_fail");
            return response.toString();
        } else {
            inventoryDAODB.removeItem(payload.getRequest_id());
            requestlog.removeRequest(payload.getRequest_id());

            ObjectNode response = mapper.createObjectNode();
            response.put("type", "remove_item_OK");
            response.put("payloadJson", gson.toJson(payload));
            handler.send(response.toString());

            InventoryDataResponse inventoryResponse = new InventoryDataResponse();
            inventoryResponse.waitingItems = inventoryDAODB.getItemsByStatus(Inventory.STATUS_WAITING);
            inventoryResponse.scheduledItems = inventoryDAODB.getItemsByStatus(Inventory.STATUS_SCHEDULED);
            inventoryResponse.inProgressItems = inventoryDAODB.getItemsByStatus(Inventory.STATUS_IN_PROGRESS);
            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(inventoryResponse));

            RequestListDataResponse requestResponse = new RequestListDataResponse();
            requestResponse.requests = requestlog.getRequestsByType("additem");
            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(requestResponse));
            return null;
        }
    }
}
"""

with open("src/main/java/com/Bidding System/backends/server/handler/ClientHandler.java", "w", encoding="utf-8") as f:
    f.write(client_handler)
with open("src/main/java/com/Bidding System/backends/server/service/AccountService.java", "w", encoding="utf-8") as f:
    f.write(account_service)
with open("src/main/java/com/Bidding System/backends/server/service/AdminService.java", "w", encoding="utf-8") as f:
    f.write(admin_service)
with open("src/main/java/com/Bidding System/backends/server/service/AuctionProcessors.java", "w", encoding="utf-8") as f:
    f.write(auction_processors)
with open("src/main/java/com/Bidding System/backends/server/service/UserService.java", "w", encoding="utf-8") as f:
    f.write(user_service)

print("Files regenerated.")

