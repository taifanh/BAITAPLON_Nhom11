package backends.client.controllers.admin;

import backends.client.controllers.ViewLoader;
import backends.client.controllers.base.BaseController;
import backends.client.controllers.components.CustomItemRequestCell;
import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import backends.common.messages.MsgAuction.AdminActionCommand;
import backends.common.messages.MsgData.FetchDataRequest;
import backends.common.messages.MsgData.RequestRecordDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class AdminRequestController extends BaseController {

    @FXML private ListView<RequestRecordDto> requestList;

    private static final String MSG_REQUEST_LIST = "REQUEST_LIST_DATA";
    private static final String MSG_ADD_ITEM_OK  = "add_item_OK";
    private static final String MSG_ACTION_OK    = "ACTION_SUCCESS";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public  final Set<String>                   selectedRequestIds = new HashSet<>();
    private final Map<String, RequestRecordDto> requestCache       = new HashMap<>();
    private final ObservableList<RequestRecordDto> pendingRequests =
            FXCollections.observableArrayList();

    private Consumer<String> requestHandler;

    @FXML
    public void initialize() {
        requestList.setCellFactory(lv -> new CustomItemRequestCell(selectedRequestIds));
        requestList.setItems(pendingRequests);
        subscribeMessages();
        fetchRequests();
    }

    @FXML
    public void handleSignOut(ActionEvent e) {
        UserSession.setCurrentAccount(null);
        navigate("SignIn.fxml", "Dang nhap", e.getSource());
    }

    private void navigate(String fxml, String title, Object source) {
        try {
            Parent root = ViewLoader.load(fxml);
            Stage stage = (Stage) ((Node) source).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void cleanup() {
        if (requestHandler != null)
            MessageBus.getInstance().unsubscribe(requestHandler);
    }

    // ── Button handlers ───────────────────────────────────────────

    @FXML
    public void handleAcceptRequests(ActionEvent event) throws IOException {
        if (selectedRequestIds.isEmpty()) {
            showAlert("Please select requests to approve.");
            return;
        }
        for (String reqId : selectedRequestIds) {
            RequestRecordDto dto    = requestCache.get(reqId);
            String           userId = dto != null ? dto.userId : null;
            AdminActionCommand msg = new AdminActionCommand("ACCEPT_REQUEST", reqId, userId);
            UserSession.getConnection().send(msg);
        }
    }

    @FXML
    public void handleRejectRequests(ActionEvent event) throws IOException {
        if (selectedRequestIds.isEmpty()) {
            showAlert("Please select requests to reject.");
            return;
        }
        for (String reqId : selectedRequestIds)
            UserSession.getConnection().send(new AdminActionCommand("REJECT_REQUEST", reqId));
    }

    // ── MessageBus ────────────────────────────────────────────────

    private void subscribeMessages() {
        requestHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                String   type = resolveType(node);
                switch (type) {
                    case MSG_REQUEST_LIST -> handleRequestListData(node);
                    case MSG_ADD_ITEM_OK  -> Platform.runLater(this::fetchRequests);
                    case MSG_ACTION_OK    -> Platform.runLater(this::fetchRequests);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(requestHandler);
    }

    private void handleRequestListData(JsonNode root) {
        JsonNode requestsNode = root.path("requests");
        List<RequestRecordDto> parsed = new ArrayList<>();

        for (JsonNode r : requestsNode) {
            RequestRecordDto dto = new RequestRecordDto();
            dto.requestId   = r.path("id").asText("");
            dto.userId      = r.path("userId").asText("");
            dto.requestType = r.path("requestType").asText("");
            dto.requestInfo = r.path("requestInfo").asText("");
            dto.time        = r.path("time").asText("");
            dto.status      = r.path("status").asText("");
            parsed.add(dto);
        }

        Platform.runLater(() -> {
            requestCache.clear();
            pendingRequests.clear();
            for (RequestRecordDto dto : parsed) {
                requestCache.put(dto.requestId, dto);
                pendingRequests.add(dto);
            }
        });
    }

    // ── Utilities ─────────────────────────────────────────────────

    private void fetchRequests() {
        selectedRequestIds.clear();
        UserSession.getConnection().send(new FetchDataRequest("FETCH_REQUESTS"));
    }

    private String resolveType(JsonNode node) {
        String t = node.path("messageType").asText("");
        return t.isBlank() ? node.path("type").asText("") : t;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
