package controllers.Server;

import Database.BidTransactions;
import Database.RequestLog;
import Database.UserStore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import controllers.AuctionService;
import controllers.UserSession;
import models.Extra.messages.*;
import models.accounts.User;
import models.bidding.BidTransaction;
import models.core.Item;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import models.items.ItemType;
import models.items.itemFactory;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;                        // field — dùng lâu dài
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);;;
    private String watchingAuctionId = null;        // client đang xem phiên nào

    public Gson gson = new Gson();

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

                // ADMIN XIN DỮ LIỆU INVENTORY
                case "FETCH_INVENTORY" -> {
                    Database.Inventory inventoryDB = new Database.Inventory();
                    InventoryDataResponse response = new InventoryDataResponse();

                    response.waitingItems = inventoryDB.getItemsByStatus(Database.Inventory.STATUS_WAITING);
                    response.scheduledItems = inventoryDB.getItemsByStatus(Database.Inventory.STATUS_SCHEDULED);
                    response.inProgressItems = inventoryDB.getItemsByStatus(Database.Inventory.STATUS_IN_PROGRESS);

                    send(mapper.writeValueAsString(response));
                }

                // LẤY THÔNG TIN CỦA AUCTION ĐƯỢC CLICK VÀO
                case "FETCH_AUCTION_STATUS" -> {
                    String itemId = node.get("itemId").asText();

                    AuctionStatusMessage statusMsg = new AuctionStatusMessage();
                    java.time.Duration remaining = AuctionService.getDuration(itemId);

                    if (!remaining.isZero() && !remaining.isNegative()) {
                        statusMsg.status = "STARTED";
                        statusMsg.itemId = itemId;
                        statusMsg.auctionId = AuctionService.getManagedActiveAuction(itemId).getAuctionId(); // hoặc lấy auctionId thật nếu cần
                        statusMsg.endTimeEpoch = System.currentTimeMillis() + remaining.toMillis();
                    } else {
                        statusMsg.status = "ENDED";
                        statusMsg.itemId = itemId;
                        statusMsg.auctionId = AuctionService.getManagedActiveAuction(itemId).getAuctionId();
                        statusMsg.endTimeEpoch = 0;
                    }
                    send(mapper.writeValueAsString(statusMsg));
                }

                // ADMIN XIN DỮ LIỆU REQUEST
                case "FETCH_REQUESTS" -> {
                    Database.RequestLog requestLogDB = new Database.RequestLog();
                    RequestListDataResponse response = new RequestListDataResponse();

                    response.requests = requestLogDB.getRequestsByType("additem");

                    send(mapper.writeValueAsString(response));
                }

                // ADMIN RA LỆNH THAO TÁC DATABASE
                case "ADMIN_ACTION" -> {
                    AdminActionCommand cmd = mapper.readValue(json, AdminActionCommand.class);
                    Database.Inventory inventoryDB = new Database.Inventory();
                    Database.RequestLog requestLogDB = new Database.RequestLog();

                    if ("SCHEDULE_ITEM".equals(cmd.action)) {
                        inventoryDB.updateItemStatus(cmd.targetId, Database.Inventory.STATUS_SCHEDULED);

                        // Báo lại cho Admin là đã xong để Admin load lại list
                        ObjectNode ack = mapper.createObjectNode();
                        ack.put("type", "ACTION_SUCCESS");
                        send(ack.toString());
                    }
                    else if ("REJECT_REQUEST".equals(cmd.action)) {
                        requestLogDB.updateRequestStatus(cmd.targetId, Database.RequestLog.STATUS_REJECTED);

                        ObjectNode ack = mapper.createObjectNode();
                        ack.put("type", "ACTION_SUCCESS");
                        send(ack.toString());
                    }
                    else if ("ACCEPT_REQUEST".equals(cmd.action)) {
                        // Tìm request trong DB
                        Database.RequestLog.RequestRecord request = requestLogDB.findByRequestId(cmd.targetId);
                        if (request != null) {
                            Createitempayload payload = gson.fromJson(request.requestInfo(), Createitempayload.class);
                            models.items.ItemType itemType = models.items.ItemType.valueOf(payload.getItemType());

                            models.core.Item item = models.items.itemFactory.createItem(
                                    itemType,
                                    payload.getItem_name(),
                                    payload.getBasePrice(),
                                    payload.getItemInfo()
                            );

                            // Lưu vào Inventory và đổi trạng thái Request
                            inventoryDB.saveItem(item, request.userId());
                            requestLogDB.updateRequestStatus(cmd.targetId, Database.RequestLog.STATUS_ACCEPTED);

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

                    UserStore userStore = new UserStore();
                    userStore.update_balance(payload.getAmount(), userId);

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
                    String userId = node.get("Id_user").asText();
                    String payloadJson = node.get("payloadJson").asText();

                    Change_infopayload payload = mapper.readValue(payloadJson,Change_infopayload.class);

                    ObjectNode responseNode = mapper.createObjectNode();
                    responseNode.put("type", "change_info_OK");
                    responseNode.put("payloadJson", gson.toJson(payload));

                    send(responseNode.toString());
                }
                case "login" -> {// xử lý định danh cho từng clienthandler
                    String userId = node.get("Id_user").asText();
                    String payloadJson = node.get("payloadJson").asText();

                    loginpayload payload = mapper.readValue(payloadJson, loginpayload.class);

                    this.userId = userId;
                    this.role = payload.getRole();

                    AuctionRoom.getInstance().connectors.put(userId, this);
                    System.out.println("User " + userId + " với role " + payload.getRole() + " đã kết nối thành công!");
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
        node.put("type", "OK");
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