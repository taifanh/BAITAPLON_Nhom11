package com.bidding_system.backends.server.service;

import com.bidding_system.backends.common.messages.MsgAuction.AuctionCommandMessage;
import com.bidding_system.backends.common.messages.MsgAuction.AuctionStatusMessage;
import com.bidding_system.backends.common.messages.MsgBid.CancelAutoBidding;
import com.bidding_system.backends.common.messages.MsgBid.ClientSendBid;
import com.bidding_system.backends.common.messages.MsgBid.RegisterAutoBidding;
import com.bidding_system.backends.common.messages.MsgBid.ServerBidRespond;
import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.server.database.BidTransactionDAO;
import com.bidding_system.backends.server.database.InventoryDAO;
import com.bidding_system.backends.server.database.UserDAO;
import com.bidding_system.backends.server.handler.*;
import com.bidding_system.backends.server.handler.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class AuctionProcessors {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static UserDAO userDAO = new UserDAO();

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
                InventoryDAO inventoryDAODB = new InventoryDAO();
                statusMsg.sellerId = inventoryDAODB.getUserIdByItemId(itemId);
            } catch (Exception e) {
                statusMsg.sellerId = "";
            }
            BidTransactionDAO bidDb = new BidTransactionDAO();
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

    public static String registerAutoBid(ClientHandler handler, JsonNode node) throws Exception {
        RegisterAutoBidding msg = mapper.treeToValue(node, RegisterAutoBidding.class);
        String auctionId = (msg.auctionId != null && !msg.auctionId.isBlank())
                ? msg.auctionId
                : handler.watchingAuctionId;

        if (auctionId == null || auctionId.isBlank()) {
            ObjectNode error = mapper.createObjectNode();
            error.put("type", "ERROR");
            error.put("message", "Không xác định được phiên đấu giá");
            return error.toString();
        }
        AutoBidEngine.getInstance().register(new AutoBidEngine.AutoBidEntry(msg.userId, msg.auctionId, msg.maxBid, msg.increment, System.currentTimeMillis()));
        ObjectNode ack = mapper.createObjectNode();
        ack.put("type", "AUTO_BID_REGISTERED");
        return ack.toString();
    }

    public static String cancelAutoBid(ClientHandler handler, JsonNode node) throws Exception {
        CancelAutoBidding msg = mapper.treeToValue(node, CancelAutoBidding.class);
        String auctionId = (msg.auctionId != null && !msg.auctionId.isBlank())
                ? msg.auctionId
                : handler.watchingAuctionId;

        if (auctionId == null || auctionId.isBlank()) {
            ObjectNode error = mapper.createObjectNode();
            error.put("type", "ERROR");
            error.put("message", "Không xác định được phiên đấu giá");
            return error.toString();
        }
        AutoBidEngine.getInstance().remove(msg.auctionId, msg.userId);
        ObjectNode ack = mapper.createObjectNode();
        ack.put("type", "AUTO_BID_CANCELLED");
        return ack.toString();
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
        return "{\"type\":\"AUCTION_LIST\",\"data\":[]}";
    }
}
