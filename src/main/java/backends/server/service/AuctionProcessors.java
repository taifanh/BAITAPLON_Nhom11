package backends.server.service;

import backends.common.messages.MsgAuction.AuctionCommandMessage;
import backends.common.messages.MsgAuction.AuctionStatusMessage;
import backends.common.messages.MsgBid.CancelAutoBidding;
import backends.common.messages.MsgBid.ClientSendBid;
import backends.common.messages.MsgBid.RegisterAutoBidding;
import backends.common.messages.MsgBid.ServerBidRespond;
import backends.server.handler.AuctionRoom;
import backends.server.handler.BidProcessor;
import backends.server.handler.ClientHandler;
import backends.server.handler.ServerAuctionManager;
import backends.common.messages.MsgBid.*;
import backends.common.models.bidding.Auction;
import backends.server.database.BidTransactionDAO;
import backends.server.database.InventoryDAO;
import backends.server.database.UserDAO;
import backends.server.handler.*;
import backends.server.policy.IncrementPolicy;
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
        String requestingUserId = node.has("userId") ? node.get("userId").asText() : null;
        Auction managedAuction = AuctionService.getManagedActiveAuction(itemId);
        java.time.Duration remaining = AuctionService.getDuration(itemId);
        AuctionStatusMessage statusMsg = new AuctionStatusMessage();
        statusMsg.itemId = itemId;
        statusMsg.auctionId = (managedAuction != null) ? managedAuction.getAuctionId() : "";
        boolean isRunning = !remaining.isZero() && !remaining.isNegative() && managedAuction != null;
        if (isRunning) {
            statusMsg.status       = "STARTED";
            statusMsg.endTimeEpoch = System.currentTimeMillis() + remaining.toMillis();

            try {
                InventoryDAO inventoryDAODB = new InventoryDAO();
                statusMsg.sellerId = inventoryDAODB.getUserIdByItemId(itemId);
            } catch (Exception e) {
                statusMsg.sellerId = "";
            }

            BidTransactionDAO bidDb   = new BidTransactionDAO();
            ServerBidRespond maxBidder = bidDb.getMaxBidder(managedAuction.getAuctionId());

            if (maxBidder != null && maxBidder.userId != null) {
                statusMsg.maxBidderAmount = maxBidder.amount;
                String name = userDAO.getNameById(maxBidder.userId);
                statusMsg.maxBidderName   = (name != null) ? name : "";
                // increment tính theo giá cao nhất hiện tại
                statusMsg.increment = IncrementPolicy.getIncrement(maxBidder.amount);
            } else {
                double startingPrice = managedAuction.getItem().getPrices();
                statusMsg.increment  = 0;
            }
            if (requestingUserId != null && !requestingUserId.isBlank()) {
                ServerBidRespond userAutoBid =
                        bidDb.getAutoBidByUser(managedAuction.getAuctionId(), requestingUserId);
                if (userAutoBid != null && userAutoBid.isAuto) {
                    statusMsg.userHasAutoBid = true;
                    statusMsg.userMaxBid     = userAutoBid.maxBid;
                }
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
        BidProcessor.getInstance().submitAutoBid(msg.userId, auctionId, msg.maxBid);
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

        BidProcessor.getInstance().cancelAutoBid(msg.userId, msg.auctionId);
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
        BidProcessor.getInstance().submitManualBid(info.id, auctionId, info.amount);
        return null;
    }

    public static String getAuctions(ClientHandler handler, JsonNode node) throws Exception {
        return "{\"type\":\"AUCTION_LIST\",\"data\":[]}";
    }
}
