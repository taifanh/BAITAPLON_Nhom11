package backends.server.service;

import backends.common.messages.Common.Createitempayload;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.RemoveRequestpayload;
import backends.common.messages.Common.SigninPayload;
import backends.common.messages.Common.SigninResponsePayload;
import backends.common.messages.Common.SignupPayload;
import backends.common.messages.MsgData.InventoryDataResponse;
import backends.common.messages.MsgData.RequestListDataResponse;
import backends.common.models.accounts.User;
import backends.common.models.core.Account;
import backends.server.database.InventoryDAO;
import backends.server.database.MyRequestDAO;
import backends.server.database.RequestLogDAO;
import backends.server.database.UserDAO;
import backends.server.handler.AuctionRoom;
import backends.server.handler.ClientHandler;
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
    private static UserDAO userDAO = new UserDAO();
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
        UserDAO userDAO = new UserDAO();
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

        String requestId = RequestLogDAO.save_request(msg);
        responseNode.put("request_id", requestId);
        
        handler.send(responseNode.toString());
        AuctionRoom.sendadmin(responseNode.toString());
        return null;
    }

    public static String removeItem(ClientHandler handler, JsonNode node) throws Exception {
        String payloadJson = node.get("payloadJson").asText();

        RemoveRequestpayload payload = mapper.readValue(payloadJson, RemoveRequestpayload.class);
        String status_item = payload.getStatus();
        RequestLogDAO requestlog = new RequestLogDAO();
        InventoryDAO inventoryDAODB = new InventoryDAO();

        if (MyRequestDAO.STATUS_IN_PROGRESS.equals(status_item)
                || MyRequestDAO.STATUS_SCHEDULED.equals(status_item)) {
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
            inventoryResponse.waitingItems = inventoryDAODB.getItemsByStatus(InventoryDAO.STATUS_WAITING);
            inventoryResponse.scheduledItems = inventoryDAODB.getItemsByStatus(InventoryDAO.STATUS_SCHEDULED);
            inventoryResponse.inProgressItems = inventoryDAODB.getItemsByStatus(InventoryDAO.STATUS_IN_PROGRESS);
            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(inventoryResponse));

            RequestListDataResponse requestResponse = new RequestListDataResponse();
            requestResponse.requests = requestlog.getRequestsByType("additem");
            AuctionRoom.getInstance().broadcast(mapper.writeValueAsString(requestResponse));
            return null;
        }
    }
}
