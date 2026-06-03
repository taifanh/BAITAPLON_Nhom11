package backends.server.service;

import backends.common.messages.Common.CreateItemPayload;
import backends.common.messages.Common.MessageType;
import backends.common.messages.MsgAuction.AdminActionCommand;
import backends.common.messages.MsgData.BidHistoryDataResponse;
import backends.common.messages.MsgData.BidHistoryRecordDto;
import backends.common.messages.MsgData.InventoryDataResponse;
import backends.common.messages.MsgData.RequestListDataResponse;
import backends.common.models.accounts.Admin;
import backends.common.models.bidding.Auction;
import backends.common.models.core.Item;
import backends.common.models.items.ItemFactory;
import backends.common.models.items.ItemType;
import backends.server.database.InventoryDAO;
import backends.server.database.BidTransactionDAO;
import backends.server.database.ItemImageDAO;
import backends.server.database.MyRequestDAO;
import backends.server.database.RequestLogDAO;
import backends.server.handler.AuctionRoom;
import backends.server.handler.ClientHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AdminService {
    private static final String ADD_ITEM_REQUEST_TYPE = MessageType.ADD_ITEM.getValue();
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
        InventoryDAO inventoryDAODB = new InventoryDAO();
        InventoryDataResponse response = new InventoryDataResponse();

        response.waitingItems = inventoryDAODB.getItemsByStatus(InventoryDAO.STATUS_WAITING);
        response.scheduledItems = inventoryDAODB.getItemsByStatus(InventoryDAO.STATUS_SCHEDULED);
        response.inProgressItems = inventoryDAODB.getItemsByStatus(InventoryDAO.STATUS_IN_PROGRESS);

        return mapper.writeValueAsString(response);
    }

    public static String fetchRequests(ClientHandler handler, JsonNode node) throws Exception {
        RequestLogDAO requestLogDAODB = new RequestLogDAO();
        RequestListDataResponse response = new RequestListDataResponse();

        response.requests = requestLogDAODB.getRequestsByType(ADD_ITEM_REQUEST_TYPE);

        return mapper.writeValueAsString(response);
    }

    public static String fetchBidHistory(ClientHandler handler, JsonNode node) throws Exception {
        String auctionId = node.path("auctionId").asText("");
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
        BidHistoryDataResponse response = new BidHistoryDataResponse();
        response.auctionId = auctionId;
        response.records = new ArrayList<>();

        if (!auctionId.isBlank()) {
            List<BidTransactionDAO.BidHistoryDisplayRecord> records =
                    bidTransactionDAO.getBidHistoryForDisplay(auctionId);
            for (BidTransactionDAO.BidHistoryDisplayRecord record : records) {
                response.records.add(new BidHistoryRecordDto(
                        record.auctionId(),
                        record.bidderId(),
                        record.bidderName(),
                        record.itemId(),
                        record.amount(),
                        record.bidTime()
                ));
            }
        }

        return mapper.writeValueAsString(response);
    }

    public static String adminAction(ClientHandler handler, JsonNode node) throws Exception {
        AdminActionCommand cmd = mapper.treeToValue(node, AdminActionCommand.class);
        InventoryDAO inventoryDAODB = new InventoryDAO();
        RequestLogDAO requestLogDAODB = new RequestLogDAO();
        MyRequestDAO myRequestDAO = new MyRequestDAO();

        if ("SCHEDULE_ITEM".equals(cmd.action)) {
            inventoryDAODB.updateItemStatus(cmd.targetId, InventoryDAO.STATUS_SCHEDULED);
            String requestId = inventoryDAODB.getRequestIdbyItem(cmd.targetId);
            // Đồng bộ DB rồi phát 1 event chung để seller-side list tự cập nhật.
            myRequestDAO.updateRequestStatus(requestId, MyRequestDAO.STATUS_SCHEDULED);
            RequestStatusNotifier.notifyByRequestId(requestId, cmd.targetId, MyRequestDAO.STATUS_SCHEDULED);

            ObjectNode ack = mapper.createObjectNode();
            ack.put("type", "ACTION_SUCCESS");
            handler.send(ack.toString());

            InventoryDataResponse inventoryResponse = new InventoryDataResponse();
            inventoryResponse.waitingItems = inventoryDAODB.getItemsByStatus(InventoryDAO.STATUS_WAITING);
            inventoryResponse.scheduledItems = inventoryDAODB.getItemsByStatus(InventoryDAO.STATUS_SCHEDULED);
            inventoryResponse.inProgressItems = inventoryDAODB.getItemsByStatus(InventoryDAO.STATUS_IN_PROGRESS);

            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(inventoryResponse));
            return null; // Already sent ack
        } else if ("REJECT_REQUEST".equals(cmd.action)) {
            requestLogDAODB.updateRequestStatus(cmd.targetId, RequestLogDAO.STATUS_REJECTED);
            // Reject chỉ cần đổi trạng thái request và thông báo lại cho đúng user.
            myRequestDAO.updateRequestStatus(cmd.targetId, RequestLogDAO.STATUS_REJECTED);
            RequestStatusNotifier.notifyByRequestId(cmd.targetId, null, RequestLogDAO.STATUS_REJECTED);

            ObjectNode ack = mapper.createObjectNode();
            ack.put("type", "ACTION_SUCCESS");
            return ack.toString();
        } else if ("ACCEPT_REQUEST".equals(cmd.action)) {
            RequestLogDAO.RequestRecord request = requestLogDAODB.findByRequestId(cmd.targetId);
            if (request != null) {
                CreateItemPayload payload = GSON.fromJson(request.requestInfo(), CreateItemPayload.class);
                ItemType itemType = ItemType.valueOf(payload.getItemType());

                Item item = ItemFactory.createItem(
                        itemType,
                        payload.getItem_name(),
                        payload.getBasePrice(),
                        payload.getItemInfo()
                );

                inventoryDAODB.saveItem(item, request.userId(), request.requestId());
                new ItemImageDAO().updateItemId(request.requestId(), item.getId());
                requestLogDAODB.removeRequest(cmd.targetId);
                // Khi accept, item mới được tạo và chuyển sang WAITING; notifier giúp list seller phản ánh ngay.
                myRequestDAO.updateRequestStatus(cmd.targetId, RequestLogDAO.STATUS_WAITING);
                RequestStatusNotifier.notifyByRequestId(cmd.targetId, item.getId(), RequestLogDAO.STATUS_WAITING);

                ObjectNode ack = mapper.createObjectNode();
                ack.put("type", "ACTION_SUCCESS");
                return ack.toString();
            }
        }
        return null;
    }
}
