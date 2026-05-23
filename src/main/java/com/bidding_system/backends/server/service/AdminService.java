package com.bidding_system.backends.server.service;

import com.bidding_system.backends.common.messages.Common.Createitempayload;
import com.bidding_system.backends.common.messages.MsgAuction.AdminActionCommand;
import com.bidding_system.backends.common.messages.MsgData.BidHistoryDataResponse;
import com.bidding_system.backends.common.messages.MsgData.BidHistoryRecordDto;
import com.bidding_system.backends.common.messages.MsgData.InventoryDataResponse;
import com.bidding_system.backends.common.messages.MsgData.RequestListDataResponse;
import com.bidding_system.backends.common.models.accounts.Admin;
import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.common.models.core.Item;
import com.bidding_system.backends.common.models.items.ItemFactory;
import com.bidding_system.backends.common.models.items.ItemType;
import com.bidding_system.backends.server.database.InventoryDAO;
import com.bidding_system.backends.server.database.BidTransactionDAO;
import com.bidding_system.backends.server.database.RequestLogDAO;
import com.bidding_system.backends.server.handler.AuctionRoom;
import com.bidding_system.backends.server.handler.ClientHandler;
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

        response.requests = requestLogDAODB.getRequestsByType("additem");

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

        if ("SCHEDULE_ITEM".equals(cmd.action)) {
            inventoryDAODB.updateItemStatus(cmd.targetId, InventoryDAO.STATUS_SCHEDULED);

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

            ObjectNode ack = mapper.createObjectNode();
            ack.put("type", "ACTION_SUCCESS");
            return ack.toString();
        } else if ("ACCEPT_REQUEST".equals(cmd.action)) {
            RequestLogDAO.RequestRecord request = requestLogDAODB.findByRequestId(cmd.targetId);
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
                requestLogDAODB.updateRequestStatus(cmd.targetId, RequestLogDAO.STATUS_WAITING);
                
                ObjectNode response = mapper.createObjectNode();
                response.put("type", "ACCEPTED_SUCCESS");
                String targetUserId = (cmd.userId != null && !cmd.userId.isBlank())
                        ? cmd.userId
                        : request.userId();
                response.put("user_id", targetUserId);
                response.put("request_id", cmd.targetId);
                response.put("status", RequestLogDAO.STATUS_WAITING);

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
