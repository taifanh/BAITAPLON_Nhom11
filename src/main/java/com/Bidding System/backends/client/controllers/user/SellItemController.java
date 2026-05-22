package backends.client.controllers.user;

import backends.client.controllers.ViewLoader;
import backends.client.controllers.base.BaseController;
import backends.client.controllers.components.CustomItemCell;
import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import backends.server.database.MyRequestDAO;
import backends.common.messages.Common.*;
import backends.common.models.accounts.User;
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
import java.util.List;
import java.util.function.Consumer;

public class SellItemController extends BaseController {

    @FXML private ListView<String> listPendingItems;

    private static final String MSG_ADD_ITEM_OK    = "add_item_OK";
    private static final String MSG_REMOVE_ITEM_OK = "remove_item_OK";
    private static final String MSG_REMOVE_FAIL    = "remove_item_fail";
    private static final String MSG_ACCEPTED        = "ACCEPTED_SUCCESS";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ObservableList<String> pendingRequestIds =
            FXCollections.observableArrayList();
    private final MyRequestDAO requestDAO = new MyRequestDAO();

    private Consumer<String> addItemHandler;
    private Consumer<String> removeItemHandler;
    private Consumer<String> acceptedHandler;

    @FXML
    public void initialize() throws IOException {
        loadPendingRequests();
        subscribeAddItem();
        subscribeRemoveItem();
        subscribeAccepted();
    }

    @Override
    public void cleanup() {
        if (addItemHandler    != null) MessageBus.getInstance().unsubscribe(addItemHandler);
        if (removeItemHandler != null) MessageBus.getInstance().unsubscribe(removeItemHandler);
        if (acceptedHandler   != null) MessageBus.getInstance().unsubscribe(acceptedHandler);
    }

    // ── Subscriptions ─────────────────────────────────────────────
    private void subscribeAddItem() {
        addItemHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                if (!MSG_ADD_ITEM_OK.equals(node.path("type").asText())) return;

                String requestId  = node.path("request_id").asText(null);
                String payloadRaw = node.path("payloadJson").asText();
                Createitempayload payload =
                        new Gson().fromJson(payloadRaw, Createitempayload.class);

                Message msg = new Message();
                msg.Id_user     = UserSession.getCurrentUser().getId();
                msg.messageType = "additem";
                msg.payloadJson = new Gson().toJson(payload);

                requestDAO.save_myrequest(msg, requestId);
                Platform.runLater(() -> {
                    if (requestId != null) pendingRequestIds.add(requestId);
                    refreshListView();
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
                    RemoveRequestpayload payload = new Gson().fromJson(
                            node.get("payloadJson").asText(),
                            RemoveRequestpayload.class);
                    String requestId = payload.getRequest_id();
                    if (requestId == null || requestId.isBlank()) return;

                    requestDAO.remove_request(requestId);
                    Platform.runLater(() -> {
                        pendingRequestIds.remove(requestId);
                        try { loadPendingRequests(); } catch (IOException e) { e.printStackTrace(); }
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
                String status    = node.path("status").asText(MyRequestDAO.STATUS_WAITING);
                User   current   = UserSession.getCurrentUser();
                if (current == null || requestId.isBlank()) return;
                if (!userId.isBlank() && !current.getId().equals(userId)) return;

                requestDAO.updateRequestStatus(requestId, status);
                Platform.runLater(() -> {
                    try { loadPendingRequests(); } catch (IOException e) { e.printStackTrace(); }
                    showAlert(Alert.AlertType.INFORMATION, "Admin accepted your item!");
                });
            } catch (Exception e) { e.printStackTrace(); }
        };
        MessageBus.getInstance().subscribe(acceptedHandler);
    }

    // ── UI helpers ────────────────────────────────────────────────
    private void loadPendingRequests() throws IOException {
        if (listPendingItems == null) return;
        User current = UserSession.getCurrentUser();
        if (current == null) return;

        List<MyRequestDAO.RequestRecord> records =
                requestDAO.getMyRequestsByType("additem");

        pendingRequestIds.clear();
        records.stream()
                .filter(r -> current.getId().equals(r.userId()))
                .filter(r -> r.requestId() != null)
                .forEach(r -> pendingRequestIds.add(r.requestId()));

        refreshListView();
    }

    private void refreshListView() {
        listPendingItems.setItems(pendingRequestIds);
        listPendingItems.setCellFactory(lv -> new CustomItemCell());
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