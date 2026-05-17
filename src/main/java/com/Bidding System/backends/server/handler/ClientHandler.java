package backends.server.handler;

import backends.common.messages.MsgBid.ServerBidRespond;
import backends.common.models.accounts.User;
import backends.common.models.bidding.Auction;
import backends.common.models.core.Account;
import backends.common.models.core.Item;
import backends.common.models.items.ItemFactory;
import backends.common.models.items.ItemType;
import backends.server.database.*;
import backends.server.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import backends.server.service.AuctionService;
import backends.common.messages.Common.*;
import backends.common.messages.MsgAuction.AdminActionCommand;
import backends.common.messages.MsgAuction.AuctionCommandMessage;
import backends.common.messages.MsgAuction.AuctionStatusMessage;
import backends.common.messages.MsgBid.ClientSendBid;
import backends.common.messages.MsgData.InventoryDataResponse;
import backends.common.messages.MsgData.RequestListDataResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;                        // field — dùng lâu dài
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private UserDAOImpl userDAOImpl = new UserDAOImpl();
    public Gson gson = new Gson();

    private String watchingAuctionId = null;        // client đang xem phiên nào
    private String userId = null;
    private String role = null;

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
                case "AUCTION_ITEMS_RESPONSE" -> {
                    AuctionRoom.getInstance().broadcast(json);
                }

                case "GET_BALANCE" -> {
                    Message msg = mapper.readValue(json, Message.class);
                    UserDAOImpl userDAOImpl = new UserDAOImpl();
                    double currentBalance = userDAOImpl.get_balance(msg.Id_user);
                    send(okJson(currentBalance));
                }
                case "signin" -> {
                    send(UserService.signin(this, node));
                }
                case "signup" ->{
                    send(UserService.signup(this, node));
                }

                // ADMIN XIN DỮ LIỆU INVENTORY
                case "FETCH_INVENTORY" -> {
                    Inventory inventoryDB = new Inventory();
                    InventoryDataResponse response = new InventoryDataResponse();

                    response.waitingItems = inventoryDB.getItemsByStatus(Inventory.STATUS_WAITING);
                    response.scheduledItems = inventoryDB.getItemsByStatus(Inventory.STATUS_SCHEDULED);
                    response.inProgressItems = inventoryDB.getItemsByStatus(Inventory.STATUS_IN_PROGRESS);

                    send(mapper.writeValueAsString(response));
                }

                // LẤY THÔNG TIN CỦA AUCTION ĐƯỢC CLICK VÀO
                case "FETCH_AUCTION_STATUS" -> {
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
                            Inventory inventoryDB = new Inventory();
                            statusMsg.sellerId = inventoryDB.getUserIdByItemId(itemId);
                        } catch (Exception e) {
                            statusMsg.sellerId = "";
                        }
                        BidTransactions bidDb = new BidTransactions();
                        ServerBidRespond maxBidder = bidDb.getMaxBidder(managedAuction.getAuctionId());
                        if (maxBidder != null && maxBidder.userId != null) {
                            statusMsg.maxBidderAmount = String.valueOf(maxBidder.amount);
                            String name = userDAOImpl.getNameById(maxBidder.userId);
                            statusMsg.maxBidderName = (name != null) ? name : "";
                        }
                    } else if (managedAuction == null) {
                        // Chưa có auction nào — item đang SCHEDULED, chưa bắt đầu
                        statusMsg.status = "NOT_STARTED";
                        statusMsg.endTimeEpoch = 0;
                    } else {
                        statusMsg.status = "ENDED";
                        statusMsg.endTimeEpoch = 0;
                    }
                    send(mapper.writeValueAsString(statusMsg));
                }

                // ADMIN XIN DỮ LIỆU REQUEST
                case "FETCH_REQUESTS" -> {
                    RequestLog requestLogDB = new RequestLog();
                    RequestListDataResponse response = new RequestListDataResponse();

                    response.requests = requestLogDB.getRequestsByType("additem");

                    send(mapper.writeValueAsString(response));
                }

                // ADMIN RA LỆNH THAO TÁC DATABASE
                case "ADMIN_ACTION" -> {
                    AdminActionCommand cmd = mapper.readValue(json, AdminActionCommand.class);
                    Inventory inventoryDB = new Inventory();
                    RequestLog requestLogDB = new RequestLog();

                    if ("SCHEDULE_ITEM".equals(cmd.action)) {
                        inventoryDB.updateItemStatus(cmd.targetId, Inventory.STATUS_SCHEDULED);

                        // Báo lại cho Admin là đã xong để Admin load lại list
                        ObjectNode ack = mapper.createObjectNode();
                        ack.put("type", "ACTION_SUCCESS");
                        send(ack.toString());
                        InventoryDataResponse inventoryResponse = new InventoryDataResponse();
                        inventoryResponse.waitingItems = inventoryDB.getItemsByStatus(Inventory.STATUS_WAITING);
                        inventoryResponse.scheduledItems = inventoryDB.getItemsByStatus(Inventory.STATUS_SCHEDULED);
                        inventoryResponse.inProgressItems = inventoryDB.getItemsByStatus(Inventory.STATUS_IN_PROGRESS);

                        AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(inventoryResponse));
                    }
                    else if ("REJECT_REQUEST".equals(cmd.action)) {
                        requestLogDB.updateRequestStatus(cmd.targetId, RequestLog.STATUS_REJECTED);

                        ObjectNode ack = mapper.createObjectNode();
                        ack.put("type", "ACTION_SUCCESS");
                        send(ack.toString());
                    }
                    else if ("ACCEPT_REQUEST".equals(cmd.action)) {
                        // Tìm request trong DB
                        RequestLog.RequestRecord request = requestLogDB.findByRequestId(cmd.targetId);
                        if (request != null) {
                            Createitempayload payload = gson.fromJson(request.requestInfo(), Createitempayload.class);
                            ItemType itemType = ItemType.valueOf(payload.getItemType());

                            Item item = ItemFactory.createItem(
                                    itemType,
                                    payload.getItem_name(),
                                    payload.getBasePrice(),
                                    payload.getItemInfo(),
                                    payload.getBidIncrement()
                            );

                            // Lưu vào Inventory và đổi trạng thái Request
                            inventoryDB.saveItem(item, request.userId(),request.id());
                            requestLogDB.updateRequestStatus(cmd.targetId, RequestLog.STATUS_WAITING);
                            // gửi tín hiệu cho usercontroller tự cập nhật dữ liệu (trạng thái của item)
                            ObjectNode response =  mapper.createObjectNode();
                            response.put("type", "ACCEPTED_SUCCESS");
                            String targetUserId = (cmd.userId != null && !cmd.userId.isBlank())
                                    ? cmd.userId
                                    : request.userId();
                            response.put("user_id" , targetUserId);
                            response.put("request_id" ,  cmd.targetId);
                            response.put("status" , RequestLog.STATUS_WAITING);

                            ClientHandler targetHandler = AuctionRoom.getInstance().connectors.get(targetUserId);
                            if (targetHandler != null) {
                                targetHandler.send(String.valueOf(response));
                            }

                            ObjectNode ack = mapper.createObjectNode();
                            ack.put("type", "ACTION_SUCCESS");
                            send(ack.toString());
                        }
                    }
                }

                // Lệnh start và end auction
                case "AUCTION_COMMAND" -> {
                    AuctionCommandMessage cmd = mapper.readValue(json, AuctionCommandMessage.class);

                    if ("START".equals(cmd.command)) {
                        System.out.println("[Server] Nhan lenh START cho item: " + cmd.itemId);
                        ServerAuctionManager.getInstance().startAuction(cmd.itemId, cmd.durationMinutes);

                    } else if ("END".equals(cmd.command)) {
                        System.out.println("[Server] Nhan lenh END ep buoc cho item: " + cmd.itemId);
                        ServerAuctionManager.getInstance().endAuction(cmd.itemId);
                    }
                }

                case "WATCH_AUCTION" -> {
                    // Client mở màn hình chi tiết phiên
                    watchingAuctionId = node.get("auctionId").asText();
                    AuctionRoom.getInstance().watch(this, watchingAuctionId);
                }

                case "UNWATCH_AUCTION" -> {
                    AuctionRoom.getInstance().unwatch(this);
                    watchingAuctionId = null;
                }

                case "PLACE_BID" -> {
                    ClientSendBid info = mapper.readValue(json, ClientSendBid.class);

                    String auctionId = (info.auctionId != null && !info.auctionId.isBlank())
                            ? info.auctionId
                            : watchingAuctionId;

                    if (auctionId == null || auctionId.isBlank()) {
                        send(errorJson("Không xác định được phiên đấu giá"));
                        return;
                    }

                    // ← Chỉ cần 1 dòng này, batch processor lo phần còn lại
                    BidBatchProcessor.getInstance().submitBid(info.id, auctionId, info.amount);

                    // ACK ngay cho client biết server đã nhận bid
                    // (kết quả thực sự sẽ broadcast sau tối đa 10 giây)
                    ObjectNode ack = mapper.createObjectNode();
                    ack.put("type", "BID_QUEUED");
                    ack.put("auctionId", auctionId);
                    ack.put("amount", info.amount);
                    send(ack.toString());
                }

                case "GET_AUCTIONS" -> {
                    // TODO: lấy danh sách phiên từ AuctionManager, gửi riêng cho client này
                    send("{\"type\":\"AUCTION_LIST\",\"data\":[]}");
                }

                case "DEPOSIT" -> {
                    String userId = node.get("Id_user").asText();
                    String payloadJson = node.get("payloadJson").asText();

                    Depositpayload payload = mapper.readValue(payloadJson, Depositpayload.class);
                    System.out.println("[Server] DEPOSIT received | userId=" + userId + " | amount=" + payload.getAmount());

                    UserDAOImpl userDAOImpl = new UserDAOImpl();
                    userDAOImpl.update_balance(payload.getAmount(), userId);
                    payload.setAmount(userDAOImpl.get_balance(userId));
                    ObjectNode responseNode = mapper.createObjectNode();// tạo 1 kiểu payloadjson để có thể dùng chung cho các phương thức khác
                    responseNode.put("type", "deposit_OK");
                    responseNode.put("payloadJson", gson.toJson(payload));

                    send(responseNode.toString());
                }
                case  "additem" -> {
                    String userId = node.get("Id_user").asText();
                    String payloadJson = node.get("payloadJson").asText();
//                    String request_type = node.get("request_type").asText();
                    // tạo lại 1 message từ message của client để có thể lưu vào request_log -> client không tự lưu vào request_log
                    Message msg = new Message();
                    msg.Id_user = userId;
                    msg.payloadJson = payloadJson;
                    msg.messageType = "additem";

                    Createitempayload payload = mapper.readValue(payloadJson, Createitempayload.class);// cần constructor rỗng
                    // response này chỉ chứa các thông tin chính của sản phẩm
                    ObjectNode responseNode = mapper.createObjectNode();
                    responseNode.put("type", "add_item_OK");
                    responseNode.put("payloadJson", gson.toJson(payload));

                    String requestId = RequestLog.save_request(msg);// save to request database waitting for admin's acceptance
                    responseNode.put("request_id", requestId);
                    send(responseNode.toString());// send back to user and admin
                    AuctionRoom.sendadmin(responseNode.toString());
                }
                case "change_info" -> {
//                    String userId = node.get("Id_user").asText();
                    String payloadJson = node.get("payloadJson").asText();

                    Change_infopayload payload = mapper.readValue(payloadJson,Change_infopayload.class);

                    ObjectNode responseNode = mapper.createObjectNode();
                    responseNode.put("type", "change_info_OK");
                    responseNode.put("payloadJson", gson.toJson(payload));

                    send(responseNode.toString());
                }
                case "removeitem" -> {
//                    String userId = node.get("Id_user").asText();
                    String payloadJson = node.get("payloadJson").asText();

                    RemoveRequestpayload payload = mapper.readValue(payloadJson, RemoveRequestpayload.class);
//                    String request_id = payload.getRequest_id();
                    String status_item = payload.getStatus();
                    RequestLog requestlog = new RequestLog();
                    Inventory inventoryDB = new  Inventory();

                    if (MyRequest.STATUS_IN_PROGRESS.equals(status_item)
                            || MyRequest.STATUS_SCHEDULED.equals(status_item)) {
                        ObjectNode response =  mapper.createObjectNode();
                        response.put("type", "remove_item_fail");
                        send (response.toString());// gửi thông baos lỗi cho usercontroller
                        return;
                    }
                    // nếu không ở trạng thái running và waitting thì xóa được
                    else {
                        inventoryDB.removeItem(payload.getRequest_id()); // find item by request_id
                        requestlog.removeRequest(payload.getRequest_id());// already have a function checking whether the request existed or not

                        ObjectNode response =  mapper.createObjectNode();
                        response.put("type", "remove_item_OK");
                        response.put("payloadJson", gson.toJson(payload));

                        send(response.toString());

                        InventoryDataResponse inventoryResponse = new InventoryDataResponse();
                        inventoryResponse.waitingItems = inventoryDB.getItemsByStatus(Inventory.STATUS_WAITING);
                        inventoryResponse.scheduledItems = inventoryDB.getItemsByStatus(Inventory.STATUS_SCHEDULED);
                        inventoryResponse.inProgressItems = inventoryDB.getItemsByStatus(Inventory.STATUS_IN_PROGRESS);
                        AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(inventoryResponse));

                        RequestListDataResponse requestResponse = new RequestListDataResponse();
                        requestResponse.requests = requestlog.getRequestsByType("additem");
                        AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(requestResponse));
                    }

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
