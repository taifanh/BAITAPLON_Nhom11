package com.bidding_system.backends.server.service;

import com.bidding_system.backends.common.messages.Common.ChangeInfoPayload;
import com.bidding_system.backends.common.messages.Common.DepositPayload;
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
        UserDAO userDAO = new UserDAO();
        double currentBalance = userDAO.get_balance(msg.Id_user);
        
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("type", "BALANCE_OK");
        responseNode.put("amount", currentBalance);
        return responseNode.toString();
    }

    public static String deposit(ClientHandler handler, JsonNode node) throws Exception {
        String userId = node.get("Id_user").asText();
        String payloadJson = node.get("payloadJson").asText();

        DepositPayload payload = mapper.readValue(payloadJson, DepositPayload.class);
        System.out.println("[Server] DEPOSIT received | userId=" + userId + " | amount=" + payload.getAmount());

        UserDAO userDAO = new UserDAO();
        userDAO.update_balance(payload.getAmount(), userId);
        payload.setAmount(userDAO.get_balance(userId));
        
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("type", "deposit_OK");
        responseNode.put("payloadJson", gson.toJson(payload));

        return responseNode.toString();
    }

    public static String changeInfo(ClientHandler handler, JsonNode node) throws Exception {
        String payloadJson = node.get("payloadJson").asText();
        ChangeInfoPayload payload = mapper.readValue(payloadJson, ChangeInfoPayload.class);

        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("type", "change_info_OK");
        responseNode.put("payloadJson", gson.toJson(payload));

        return responseNode.toString();
    }
}
