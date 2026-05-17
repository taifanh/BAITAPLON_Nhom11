package backends.server.service;

import backends.common.messages.Common.SigninPayload;
import backends.common.messages.Common.SigninResponsePayload;
import backends.common.messages.Common.SignupPayload;
import backends.common.models.accounts.User;
import backends.common.models.core.Account;
import backends.server.database.UserDAOImpl;
import backends.server.handler.AuctionRoom;
import backends.server.handler.ClientHandler;
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
    private static UserDAOImpl userDAOImpl = new UserDAOImpl();
    public static Gson gson = new Gson();

    public static String signin(ClientHandler clientHandler, JsonNode node) throws IOException {
        SigninPayload payload = mapper.readValue(node.get("payloadJson").asText(), SigninPayload.class);
        Optional<Account> accountOptional =
                userDAOImpl.authenticate(payload.getPhoneNumber(), payload.getPassword());

        if (accountOptional.isEmpty()) {
            ObjectNode fail = mapper.createObjectNode();
            fail.put("type", "SIGNIN_FAIL");
            return fail.toString();
        }

        Account account = accountOptional.get();
        clientHandler.setUserId(account.getId());
        clientHandler.setRole(account.getRole());
        AuctionRoom.getInstance().connectors.put(clientHandler.getUserId(), clientHandler);// lưu thông tin clienthandler khi sign in thành công

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
        UserDAOImpl userDAOImpl = new UserDAOImpl();
        if (userDAOImpl.phoneNumberExists(payload.getPhoneNumber())) {
            ObjectNode fail = mapper.createObjectNode();
            fail.put("type", "SIGNUP_FAIL");
            return fail.toString();
        }
        userDAOImpl.saveUser(new User(payload.getName(),  payload.getEmail(), payload.getPhoneNumber(), payload.getPassword()));

        ObjectNode success = mapper.createObjectNode();
        success.put("type", "SIGNUP_OK");
        return success.toString();
    }
}
