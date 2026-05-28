package com.bidding_system.backends.client.controllers.admin;

import com.bidding_system.backends.client.controllers.ViewLoader;
import com.bidding_system.backends.client.controllers.base.BaseController;
import com.bidding_system.backends.client.network.MessageBus;
import com.bidding_system.backends.client.session.UserSession;
import com.bidding_system.backends.client.controllers.util.ItemJsonParser;
import com.bidding_system.backends.common.messages.MsgAuction.AdminActionCommand;
import com.bidding_system.backends.common.messages.MsgData.FetchDataRequest;
import com.bidding_system.backends.common.models.core.Item;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class AdminInventoryController extends BaseController {

    @FXML private ListView<Item> inventory;
    @FXML private Label          errorCreateAuction;

    private static final String MSG_INVENTORY = "INVENTORY_DATA";
    private static final String MSG_ACTION_OK = "ACTION_SUCCESS";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Consumer<String> inventoryHandler;

    @FXML
    public void initialize() {
        inventory.setCellFactory(lv -> createItemCell());
        subscribeMessages();
        fetchInventory();
    }

    @FXML
    public void handleSignOut(ActionEvent e) throws IOException {
        super.handleSignOut(e);
    }

    public void cleanup() {
        if (inventoryHandler != null)
            MessageBus.getInstance().unsubscribe(inventoryHandler);
    }

    // ── Button handler ────────────────────────────────────────────

    @FXML
    public void handleCreateAuction(ActionEvent event) throws IOException {
        errorCreateAuction.setVisible(false);
        Item selected = inventory.getSelectionModel().getSelectedItem();
        if (selected == null) {
            errorCreateAuction.setVisible(true);
            return;
        }
        UserSession.getConnection().send(new AdminActionCommand("SCHEDULE_ITEM", selected.getId()));
        inventory.getSelectionModel().clearSelection();
    }

    // ── MessageBus ────────────────────────────────────────────────

    private void subscribeMessages() {
        inventoryHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                String   type = resolveType(node);
                switch (type) {
                    case MSG_INVENTORY -> handleInventoryData(node);
                    case MSG_ACTION_OK -> Platform.runLater(this::fetchInventory);
                }
            } catch (Exception e) { e.printStackTrace(); }
        };
        MessageBus.getInstance().subscribe(inventoryHandler);
    }

    private void handleInventoryData(JsonNode root) {
        List<Item> waitingItems = ItemJsonParser.parse(root.path("waitingItems"));
        Platform.runLater(() ->
                inventory.setItems(FXCollections.observableArrayList(waitingItems)));
    }

    // ── Utilities ─────────────────────────────────────────────────

    public void fetchInventory() {
        UserSession.getConnection().send(new FetchDataRequest("FETCH_INVENTORY"));
    }

    private String resolveType(JsonNode node) {
        String t = node.path("messageType").asText("");
        return t.isBlank() ? node.path("type").asText("") : t;
    }

    private ListCell<Item> createItemCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText("Name: "  + item.getName()   + "\n" +
                        "Price: " + item.getPrices() + "\n" +
                        "Type: "  + item.getType()   + "\n" +
                        "Desc: "  + item.getInfo());
            }
        };
    }
}
