package com.bidding_system.backends.client.controllers.user;

import com.bidding_system.backends.client.controllers.ViewLoader;
import com.bidding_system.backends.client.controllers.base.BaseController;
import com.bidding_system.backends.client.controllers.components.CustomItemCell;
import com.bidding_system.backends.client.network.MessageBus;
import com.bidding_system.backends.client.session.UserSession;
import com.bidding_system.backends.common.constants.Statuses;
import com.bidding_system.backends.common.messages.Common.*;
import com.bidding_system.backends.common.messages.MsgData.FetchUserRequestsRequest;
import com.bidding_system.backends.common.messages.MsgData.RequestRecordDto;
import com.bidding_system.backends.common.messages.MsgData.UserRequestListResponse;
import com.bidding_system.backends.common.models.accounts.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class SellItemController extends BaseController {

    @FXML private ListView<RequestRecordDto> listPendingItems;

    private static final String MSG_ADD_ITEM_OK    = "add_item_OK";
    private static final String MSG_REMOVE_ITEM_OK = "remove_item_OK";
    private static final String MSG_REMOVE_FAIL    = "remove_item_fail";
    private static final String MSG_ACCEPTED        = "ACCEPTED_SUCCESS";
    private static final String MSG_LOADREQUEST     = "USER_REQUEST_LIST_DATA";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ObservableList<RequestRecordDto> pendingRequestIds =
            FXCollections.observableArrayList();

    private Consumer<String> addItemHandler;
    private Consumer<String> removeItemHandler;
    private Consumer<String> acceptedHandler;
    private Consumer<String> loadUserHandler;
    @FXML
    public void initialize() throws IOException {
        loadUserRequest();
        subsribeloadPendingRequests();
        subscribeAddItem();
        subscribeRemoveItem();
        subscribeAccepted();

    }

    @Override
    public void cleanup() {
        if (addItemHandler    != null) MessageBus.getInstance().unsubscribe(addItemHandler);
        if (removeItemHandler != null) MessageBus.getInstance().unsubscribe(removeItemHandler);
        if (acceptedHandler   != null) MessageBus.getInstance().unsubscribe(acceptedHandler);
        if (loadUserHandler   != null) MessageBus.getInstance().unsubscribe(loadUserHandler);
    }

    // ── Subscriptions ─────────────────────────────────────────────
    private void subscribeAddItem() {
        addItemHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                if (!MSG_ADD_ITEM_OK.equals(node.path("type").asText())) return;

                String requestId = node.path("request_id").asText(null);
                if (requestId == null || requestId.isBlank()) return;

                Platform.runLater(() -> {
                    try { loadUserRequest(); } catch (IOException e) { e.printStackTrace(); }
                });
            } catch (Exception e) { e.printStackTrace(); }
        };
        MessageBus.getInstance().subscribe(addItemHandler);
    }

    private void subscribeRemoveItem() {
        removeItemHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                String type   = node.path("type").asText();

                if (MSG_REMOVE_ITEM_OK.equals(type) && node.has("payloadJson")) {
                    RemoveRequestPayload payload = new Gson().fromJson(
                            node.get("payloadJson").asText(),
                            RemoveRequestPayload.class);
                    String requestId = payload.getRequest_id();
                    if (requestId == null || requestId.isBlank()) return;

                    Platform.runLater(() -> {
                        try { loadUserRequest(); } catch (IOException e) { e.printStackTrace(); }
                        showAlert(Alert.AlertType.INFORMATION, "Item removed successfully");
                    });
                } else if (MSG_REMOVE_FAIL.equals(type)) {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.WARNING,
                                    "Cannot remove: item is in auction or has already been processed."));
                }
            } catch (Exception e) { e.printStackTrace(); }
        };
        MessageBus.getInstance().subscribe(removeItemHandler);
    }

    private void subscribeAccepted() {
        acceptedHandler = raw -> {
            try {
                JsonNode node    = MAPPER.readTree(raw);
                if (!MSG_ACCEPTED.equals(node.path("type").asText())) return;

                String requestId = node.path("request_id").asText("");
                String userId    = node.path("user_id").asText("");
                String status    = node.path("status").asText(Statuses.WAITING);

                User   current   = UserSession.getCurrentUser();
                if (current == null || requestId.isBlank()) return;
                if (!userId.isBlank() && !current.getId().equals(userId)) return;

                Platform.runLater(() -> {
                    try { loadUserRequest(); } catch (IOException e) { e.printStackTrace(); }
                    showAlert(Alert.AlertType.INFORMATION, "Admin accepted your item!");
                });
            } catch (Exception e) { e.printStackTrace(); }
        };
        MessageBus.getInstance().subscribe(acceptedHandler);
    }

    // ── UI helpers ────────────────────────────────────────────────
    private void subsribeloadPendingRequests() throws IOException {
        loadUserHandler = rawJson -> {

            try {
                JsonNode node = MAPPER.readTree(rawJson);
                if(!MSG_LOADREQUEST.equals(node.path("type").asText())) return;

                UserRequestListResponse response = MAPPER.readValue(rawJson, UserRequestListResponse.class);

                Platform.runLater(() -> {
                    pendingRequestIds.clear();

                    if (response.requests != null) {
                        pendingRequestIds.addAll(response.requests);
                    }

                    refreshListView();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(loadUserHandler);
    }


    private void refreshListView() {
        listPendingItems.setItems(pendingRequestIds);
        listPendingItems.setCellFactory(lv -> new CustomItemCell());
    }
    public void loadUserRequest() throws IOException {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        Message msg = new Message();
        msg.Id_user = currentUser.getId();
        msg.messageType = "FETCH_USER_REQUEST";
        msg.payloadJson = MAPPER.writeValueAsString(new FetchUserRequestsRequest(currentUser.getId(), "additem"));
        UserSession.getConnection().send(msg);
    }

    @FXML
    public void handleCreateItem(ActionEvent event) throws IOException {
        FXMLLoader loader = ViewLoader.loader("CreateItem.fxml");
        Stage popup = new Stage();
        popup.setScene(new Scene(loader.load()));
        popup.setTitle("Create Item");
        popup.centerOnScreen();
        popup.show();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
