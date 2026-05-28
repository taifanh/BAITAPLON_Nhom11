package backends.client.controllers.user;

import backends.client.controllers.base.BaseController;
import backends.client.controllers.components.BidHistoryRow;
import backends.client.controllers.components.CustomBidHistoryCell;
import backends.client.controllers.util.ItemJsonParser;
import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import backends.common.messages.MsgAuction.AuctionResultMessage;
import backends.common.messages.MsgAuction.AuctionStatusMessage;
import backends.common.messages.MsgAuction.StartAuctionMessage;
import backends.common.messages.MsgBid.*;
import backends.common.messages.MsgBid.*;
import backends.common.messages.MsgAuction.*;
import backends.common.messages.MsgData.BidHistoryDataResponse;
import backends.common.messages.MsgData.BidHistoryRecordDto;
import backends.common.messages.MsgData.FetchBidHistoryRequest;
import backends.common.messages.MsgData.FetchDataRequest;
import backends.common.models.core.Item;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BiddingSpaceController extends BaseController {

    // ── FXML fields ───────────────────────────────────────────────
    @FXML private ListView<Item> listAuctionItems;
    @FXML private TextField fieldHighBidder;
    @FXML private TextField fieldCurrentAmount;
    @FXML private TextField fieldBidPrice;
    @FXML private TextField fieldBasePrice;
    @FXML private TextField fieldIncrement;
    @FXML private TextField fieldItemName;
    @FXML private TextField fieldNextMinimumBid;
    @FXML private TextField fieldMaxLimit;
    @FXML private Label     labelTimer;
    @FXML private Button    buttonPlaceBid;
    @FXML private Button    buttonAutoBid;
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private TableView<BidHistoryRow> bidHistoryTable;

    // ── Constants ─────────────────────────────────────────────────
    private static final String MSG_INVENTORY_DATA  = "INVENTORY_DATA";
    private static final String MSG_AUCTION_STATUS  = "AUCTION_STATUS";
    private static final String MSG_START_AUCTION   = "START_AUCTION";
    private static final String MSG_RECEIVE_BID     = "RECEIVE_BID";
    private static final String MSG_BID_QUEUED      = "BID_QUEUED";
    private static final String MSG_AUCTION_RESULT  = "AUCTION_RESULT";
    private static final String MSG_AUTO_REGISTERED = "AUTO_BID_REGISTERED";
    private static final String MSG_AUTO_CANCELLED  = "AUTO_BID_CANCELLED";
    private static final String MSG_BID_HISTORY     = "BID_HISTORY_DATA";
    private static final String MSG_PLACE_BID_FAILED     = "PLACE_BID_FAILED";
    private static final String MSG_AUTO_BID_FAILED     = "AUTO_BID_FAILED";


    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ── Auction state ─────────────────────────────────────────────
    private final ObservableList<Item> auctionItems = FXCollections.observableArrayList();
    private final Map<String, Long>   endTimeByItemId    = new HashMap<>();
    private final Map<String, String> auctionIdByItemId  = new HashMap<>();
    private final Map<String, Boolean> autoBidStateByAuctionId = new HashMap<>();
    private final Map<String, Double> maxLimitByAuctionId = new HashMap<>();

    private Item   selectedItem;
    private String currentAuctionId;
    private String currentSellerId;
    private double startingPrice;
    private double currentBidIncrement;
    private double currentHighestBid;
    private volatile LocalDateTime auctionEndTime;
    private boolean isAutoBidActive = false;

    private Timeline countdownTimeline;
    private Consumer<String> messageBusHandler;
    private XYChart.Series<Number, Number> priceSeries;

    // ── Lifecycle ─────────────────────────────────────────────────
    @FXML
    public void initialize() {
        configureReadOnlyFields();
        setupBidVisuals();
        setupItemListView();
        startCountdownTimer();
        subscribeToMessages();
        UserSession.getConnection().send(new FetchDataRequest("FETCH_INVENTORY"));
    }

    @Override
    public void cleanup() {
        if (messageBusHandler != null)
            MessageBus.getInstance().unsubscribe(messageBusHandler);
        if (countdownTimeline != null)
            countdownTimeline.stop();
    }

    // ── Setup ─────────────────────────────────────────────────────
    private void configureReadOnlyFields() {
        fieldHighBidder.setEditable(false);
        fieldCurrentAmount.setEditable(false);
        fieldItemName.setEditable(false);
        fieldBasePrice.setEditable(false);
        fieldIncrement.setEditable(false);
        fieldNextMinimumBid.setEditable(false);
    }

    private void setupItemListView() {
        listAuctionItems.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText("Name: "    + item.getName()   + "\n" +
                        "Opening: " + item.getPrices() + "\n" +
                        "Type: "    + item.getType()   + "\n" +
                        "Desc: "    + item.getInfo());
            }
        });
        listAuctionItems.setItems(auctionItems);
        listAuctionItems.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, selected) -> onItemSelected(selected));
    }

    private void startCountdownTimer() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickCountdown()));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    // ── Item selection ────────────────────────────────────────────
    private void onItemSelected(Item item) {
        if (item == null) return;
        selectedItem         = item;
        currentAuctionId     = null;
        currentSellerId      = null;
        currentHighestBid    = 0;
        auctionEndTime       = null;
        applyItemDetails(item);
        fieldHighBidder.setText("Loading...");
        fieldCurrentAmount.setText("Loading...");
        clearBidVisuals("Select an active auction to view bid history.");
        buttonPlaceBid.setDisable(true);
        fieldBidPrice.setDisable(true);
        applyAutoBidInactive();
        restoreAuctionStateIfAvailable(item);
        requestAuctionStatus(item.getId());
    }

    private void restoreAuctionStateIfAvailable(Item item) {
        Long   epoch     = endTimeByItemId.get(item.getId());
        String auctionId = auctionIdByItemId.get(item.getId());
        if (epoch == null || auctionId == null) return;

        long remaining = epoch - System.currentTimeMillis();
        if (remaining <= 0) return;

        auctionEndTime   = Instant.ofEpochMilli(epoch)
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        currentAuctionId = auctionId;
        buttonPlaceBid.setDisable(false);
        fieldBidPrice.setDisable(false);
        fieldBidPrice.clear();
    }

    private void requestAuctionStatus(String itemId) {
        var req = MAPPER.createObjectNode();
        req.put("type",   "FETCH_AUCTION_STATUS");
        req.put("itemId", itemId);
        UserSession.getConnection().send(req);
    }

    // ── Message handling ──────────────────────────────────────────
    private void subscribeToMessages() {
        messageBusHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                String   type = resolveType(node);
                switch (type) {
                    case MSG_INVENTORY_DATA  -> handleInventoryData(node);
                    case MSG_AUCTION_STATUS  -> handleAuctionStatus(raw);
                    case MSG_START_AUCTION   -> handleStartAuction(raw);
                    case MSG_RECEIVE_BID     -> handleReceiveBid(raw);
                    case MSG_BID_QUEUED      -> handleBidQueued(node);
                    case MSG_AUCTION_RESULT  -> handleAuctionResult(raw);
                    case MSG_AUTO_REGISTERED -> Platform.runLater(this::applyAutoBidActive);
                    case MSG_AUTO_CANCELLED  -> handleAutoBidCancelled(raw);
                    case MSG_BID_HISTORY     -> handleBidHistoryData(raw);
                    case MSG_PLACE_BID_FAILED -> handlePlaceBidFailed(raw);
                    case MSG_AUTO_BID_FAILED -> handleAutoBidFailed(raw);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(messageBusHandler);
    }

    private void handleInventoryData(JsonNode node) {
        List<Item> items = new ArrayList<>();
        items.addAll(ItemJsonParser.parse(node.path("scheduledItems")));
        items.addAll(ItemJsonParser.parse(node.path("inProgressItems")));
        Platform.runLater(() -> auctionItems.setAll(items));
    }

    private void handleAuctionStatus(String raw) throws Exception {
        AuctionStatusMessage msg = MAPPER.readValue(raw, AuctionStatusMessage.class);
        Platform.runLater(() -> {
            switch (msg.status) {
                case "STARTED" -> {
                    endTimeByItemId.put(msg.itemId, msg.endTimeEpoch);
                    auctionIdByItemId.put(msg.itemId, msg.auctionId);
                    if (isSelectedItem(msg.itemId)) applyStartedStatus(msg);
                }
                case "NOT_STARTED" -> {
                    if (isSelectedItem(msg.itemId)) applyNotStartedStatus();
                }
                case "ENDED" -> {
                    endTimeByItemId.remove(msg.itemId);
                    auctionIdByItemId.remove(msg.itemId);
                    if (isSelectedItem(msg.itemId)) applyEndedStatus(msg.itemId);
                }
            }
        });
    }

    private void handleStartAuction(String raw) throws Exception {
        StartAuctionMessage msg = MAPPER.readValue(raw, StartAuctionMessage.class);
        Platform.runLater(() -> {
            if (selectedItem == null ||
                    !selectedItem.getId().equals(msg.auctionId)) return;
            currentAuctionId = msg.auctionId;
            currentSellerId  = msg.sellerId;
            startingPrice    = msg.startingPrice;
            if (msg.endAt != null) auctionEndTime = msg.endAt;
            fieldItemName.setText(msg.itemName);
            fieldBasePrice.setText(String.valueOf(msg.startingPrice));
            buttonPlaceBid.setDisable(false);
            fieldBidPrice.setDisable(false);
        });
    }

    private void handleReceiveBid(String raw) throws Exception {
        ReceiveMaxBidder msg = MAPPER.readValue(raw, ReceiveMaxBidder.class);
        if (currentAuctionId == null ||
                !currentAuctionId.equals(msg.maxBidder.auctionId)) return;
        currentBidIncrement = msg.currentIncrement;
        Platform.runLater(() -> {
            fieldHighBidder.setText(msg.maxBidder.name);
            fieldCurrentAmount.setText(String.valueOf(msg.maxBidder.amount));
            fieldNextMinimumBid.setText(String.valueOf(msg.maxBidder.amount + currentBidIncrement));
            fieldIncrement.setText(String.valueOf(currentBidIncrement));
            currentHighestBid = msg.maxBidder.amount;
            requestBidVisuals(currentAuctionId);
            buttonPlaceBid.setDisable(false);
            fieldBidPrice.setDisable(false);
            fieldBidPrice.clear();
        });
    }

    private void handleBidQueued(JsonNode node) {
        double amount = node.get("amount").asDouble();
        Platform.runLater(() -> {
            fieldBidPrice.setStyle("-fx-border-color: orange;");
            fieldBidPrice.setText("Queued: " + amount);
            buttonPlaceBid.setDisable(true);
            fieldBidPrice.setDisable(true);
        });
    }

    private void handleAuctionResult(String raw) throws Exception {
        AuctionResultMessage result = MAPPER.readValue(raw, AuctionResultMessage.class);
        Platform.runLater(() -> {
            String userId = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getId() : null;
            if (!result.hasBidder) {
                showAlert(Alert.AlertType.INFORMATION, "Auction ended with no bids.");
                return;
            }
            if (result.winnerId.equals(userId)) {
                showAlert(Alert.AlertType.INFORMATION,
                        "Congratulations! You won!\n" +
                                "Item: "   + result.itemName     + "\n" +
                                "Amount: " + result.winningAmount);
            } else {
                showAlert(Alert.AlertType.INFORMATION,
                        "Winner: " + result.winnerName   + "\n" +
                                "Amount: " + result.winningAmount + "\n" +
                                "Item: "   + result.itemName);
            }
        });
    }

    // ── Status helpers ────────────────────────────────────────────
    private void applyStartedStatus(AuctionStatusMessage msg) {
        currentAuctionId    = msg.auctionId;
        currentBidIncrement = msg.increment;
        currentSellerId     = msg.sellerId;
        auctionEndTime      = Instant.ofEpochMilli(msg.endTimeEpoch)
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        buttonPlaceBid.setDisable(false);
        fieldBidPrice.setDisable(false);
        currentBidIncrement = msg.increment;
        applyItemDetails(selectedItem);
        if (msg.maxBidderName != null && !msg.maxBidderName.isBlank()) {
            fieldHighBidder.setText(msg.maxBidderName);
            fieldIncrement.setText(String.valueOf(currentBidIncrement));
            fieldCurrentAmount.setText(String.valueOf(msg.maxBidderAmount));
            fieldNextMinimumBid.setText(String.valueOf(msg.maxBidderAmount + currentBidIncrement));
            try { currentHighestBid = Double.parseDouble(String.valueOf(msg.maxBidderAmount)); }
            catch (NumberFormatException e) { currentHighestBid = startingPrice; }
        } else {
            fieldHighBidder.setText("No bids yet");
            fieldCurrentAmount.setText(String.valueOf(startingPrice));
            currentHighestBid = startingPrice;
            currentBidIncrement = msg.increment;
            fieldIncrement.setText(String.valueOf(currentBidIncrement));
            fieldNextMinimumBid.setText(String.valueOf(startingPrice + currentBidIncrement));
        }
        boolean wasAutoBidActive = autoBidStateByAuctionId.getOrDefault(currentAuctionId, false);
        if (wasAutoBidActive) {
            Double savedLimit = maxLimitByAuctionId.get(currentAuctionId);
            if (savedLimit != null) fieldMaxLimit.setText(String.valueOf(savedLimit));
            applyAutoBidActive();
        } else {
            fieldMaxLimit.clear();
            buttonPlaceBid.setDisable(false);
            fieldBidPrice.setDisable(false);
        }
        requestBidVisuals(currentAuctionId);
    }

    private void applyNotStartedStatus() {
        auctionEndTime   = null;
        currentAuctionId = null;
        resetClock();
        fieldMaxLimit.clear();
        applyAutoBidInactive();
        fieldHighBidder.setText("Not started");
        fieldCurrentAmount.setText(String.valueOf(startingPrice));
        clearBidVisuals("Auction has not started.");
        buttonPlaceBid.setDisable(true);
        fieldBidPrice.setDisable(true);
    }

    private void applyEndedStatus(String itemId) {
        currentAuctionId = null;
        auctionEndTime   = null;
        resetClock();
        applyAutoBidInactive();
        fieldMaxLimit.clear();
        fieldHighBidder.setText("Auction ended");
        fieldCurrentAmount.clear();
        fieldNextMinimumBid.clear();
        fieldBasePrice.clear();
        fieldItemName.clear();
        fieldIncrement.clear();
        clearBidVisuals("Auction ended.");
        buttonPlaceBid.setDisable(true);
        fieldBidPrice.setDisable(true);
        UserSession.getConnection().send(new FetchDataRequest("FETCH_INVENTORY"));
    }

    private void setupBidVisuals() {
        priceSeries = new XYChart.Series<>();
        priceChart.getData().setAll(priceSeries);
        priceChart.setAnimated(false);
        priceChart.setCreateSymbols(true);
        CustomBidHistoryCell.configureTable(bidHistoryTable);
        bidHistoryTable.setItems(FXCollections.observableArrayList());
    }

    private void clearBidVisuals(String placeholder) {
        if (priceSeries != null) {
            priceSeries.getData().clear();
        }
        if (bidHistoryTable != null) {
            bidHistoryTable.getItems().clear();
            bidHistoryTable.setPlaceholder(new Label(placeholder));
        }
    }

    private void requestBidVisuals(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            clearBidVisuals("No auction selected.");
            return;
        }
        bidHistoryTable.setPlaceholder(new Label("Loading bid history..."));
        UserSession.getConnection().send(new FetchBidHistoryRequest(auctionId));
    }

    private void handleBidHistoryData(String raw) throws Exception {
        BidHistoryDataResponse response = MAPPER.readValue(raw, BidHistoryDataResponse.class);
        if (currentAuctionId == null || !currentAuctionId.equals(response.auctionId)) {
            return;
        }
        Platform.runLater(() -> applyBidHistoryData(response));
    }

    private void applyBidHistoryData(BidHistoryDataResponse response) {
        priceSeries.getData().clear();
        ObservableList<BidHistoryRow> rows = FXCollections.observableArrayList();
        int index = 1;
        if (response.records != null) {
            for (BidHistoryRecordDto record : response.records) {
                priceSeries.getData().add(new XYChart.Data<>(index, record.amount));
                rows.add(0, CustomBidHistoryCell.toRow(index, record));
                index++;
            }
        }
        if (rows.isEmpty()) {
            bidHistoryTable.setPlaceholder(new Label("No bids yet."));
        }
        bidHistoryTable.setItems(rows);
    }

    private void applyAutoBidActive() {
        isAutoBidActive = true;
        if (currentAuctionId != null)
            autoBidStateByAuctionId.put(currentAuctionId, true);
        buttonPlaceBid.setDisable(true);
        fieldBidPrice.setDisable(true);
        fieldMaxLimit.setEditable(false);
        buttonAutoBid.setText("STOP");
        buttonAutoBid.setStyle(
                "-fx-background-color:#dc2626;-fx-text-fill:white;" +
                        "-fx-background-radius:12;-fx-border-radius:12;-fx-font-weight:bold;");
    }

    private void handleAutoBidCancelled(String raw) throws Exception {
        AutoBiddingCancelled msg = MAPPER.readValue(raw, AutoBiddingCancelled.class);
        Platform.runLater(() -> {
            applyAutoBidInactive();
            if (msg.message != null && !msg.message.isBlank()) {
                showAlert(Alert.AlertType.INFORMATION, msg.message);
            }

        });
    }

    private void applyAutoBidInactive() {
        isAutoBidActive = false;
        if (currentAuctionId != null)
            autoBidStateByAuctionId.put(currentAuctionId, false);
        buttonPlaceBid.setDisable(false);
        fieldBidPrice.setDisable(false);
        fieldMaxLimit.setEditable(true);
        buttonAutoBid.setText("AUTO");
        buttonAutoBid.setStyle(
                "-fx-background-color:#ea580c;-fx-text-fill:white;" +
                        "-fx-background-radius:12;-fx-border-radius:12;-fx-font-weight:bold;");
    }

    // ── Button actions ────────────────────────────────────────────
    @FXML
    public void handlePlaceBid(ActionEvent event) {
        try {
            validateAuctionActive();
            double amount = parseAndValidateBidAmount(fieldBidPrice.getText());
            validateBidAmount(amount);
            String userId = null;
            if (UserSession.getCurrentUser() != null) {
                userId = UserSession.getCurrentUser().getId();
            }
            else {
                throw new RuntimeException("This user is not exist");
            }
            UserSession.getConnection().send(
                    new ClientSendBid( userId,
                            amount, currentAuctionId));
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    public void handleAutoBid(ActionEvent event) {
        if ("AUTO".equals(buttonAutoBid.getText())) {
            registerAutoBid();
        } else {
            cancelAutoBid();
        }
    }

    private void registerAutoBid() {
        try {
            validateAuctionActive();
            double maxLimit  = parsePositive(fieldMaxLimit.getText());
            double balance;
            if (UserSession.getCurrentUser() != null) {
                balance = UserSession.getCurrentUser().getBalance();
            }
            else {
                throw new RuntimeException("This user is not exist");
            }
            if (maxLimit > balance)
                throw new IllegalArgumentException("Balance insufficient");
            if (maxLimit < currentHighestBid + currentBidIncrement)
                throw new IllegalArgumentException("Max limit too low");
            if (UserSession.getCurrentUser().getId().equals(currentSellerId))
                throw new IllegalArgumentException("Cannot bid on your own item");
            maxLimitByAuctionId.put(currentAuctionId, maxLimit);
            String userId = UserSession.getCurrentUser().getId();
            UserSession.getConnection().send(new RegisterAutoBidding(currentAuctionId, userId , maxLimit));
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private void cancelAutoBid() {
        try {
            var msg = new CancelAutoBidding();
            msg.auctionId = currentAuctionId;
            if (UserSession.getCurrentUser() != null) {
                msg.userId    = UserSession.getCurrentUser().getId();
            }
            else {
                throw new RuntimeException("This user is not exist");
            }
            UserSession.getConnection().send(msg);
        }
        catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private void handlePlaceBidFailed(String raw) {
        try {
            PlaceBidFailed message = MAPPER.readValue(raw, PlaceBidFailed.class);
            String reason = message.reason;
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, reason));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleAutoBidFailed(String raw) {
        try {
            AutoBidFailed message = MAPPER.readValue(raw, AutoBidFailed.class);
            Platform.runLater(() -> {
                applyAutoBidInactive();
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, message.reason)
                );
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // ── Validation ────────────────────────────────────────────────
    private void validateAuctionActive() {
        if (selectedItem == null)
            throw new IllegalArgumentException("Please select an item");
        if (auctionEndTime == null || currentAuctionId == null)
            throw new IllegalArgumentException("No active auction");
        if (!java.time.Duration.between(LocalDateTime.now(), auctionEndTime).isPositive())
            throw new IllegalArgumentException("Auction has expired");
    }

    private double parseAndValidateBidAmount(String text) {
        try {
            double v = Double.parseDouble(text);
            if (v <= 0) throw new IllegalArgumentException("Amount must be positive");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format");
        }
    }

    private void validateBidAmount(double amount) {
        if (isAutoBidActive)
            throw new IllegalArgumentException("Disable auto-bid first");
        if (UserSession.getCurrentUser().getId().equals(currentSellerId))
            throw new IllegalArgumentException("Cannot bid on your own item");
        if (amount > UserSession.getCurrentUser().getBalance())
            throw new IllegalArgumentException("Balance insufficient");
        double minimumBid = currentHighestBid + currentBidIncrement;
        if (amount < minimumBid)
            throw new IllegalArgumentException("Minimum next bid: " + minimumBid);
    }

    private double parsePositive(String text) {
        try {
            double v = Double.parseDouble(text);
            if (v <= 0) throw new IllegalArgumentException("max limit" + " must be positive");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + "max limit");
        }
    }

    // ── Clock ─────────────────────────────────────────────────────
    private void tickCountdown() {
        if (auctionEndTime == null) { resetClock(); return; }
        var remaining = java.time.Duration.between(LocalDateTime.now(), auctionEndTime);
        if (!remaining.isPositive()) { resetClock(); return; }
        long h = remaining.toHours(), m = remaining.toMinutesPart(), s = remaining.toSecondsPart();
        Platform.runLater(() -> {
            labelTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
            labelTimer.setTextFill(Color.web("#fbbf24"));
        });
    }

    private void resetClock() {
        Platform.runLater(() -> {
            labelTimer.setText("00:00:00");
            labelTimer.setTextFill(Color.RED);
        });
    }

    // ── Utilities ─────────────────────────────────────────────────
    private boolean isSelectedItem(String itemId) {
        return selectedItem != null && selectedItem.getId().equals(itemId);
    }

    private void applyItemDetails(Item item) {
        if (item == null) return;
        fieldItemName.setText(item.getName());
        fieldBasePrice.setText(String.valueOf(item.getPrices()));
        startingPrice       = item.getPrices();
    }

    private String resolveType(JsonNode node) {
        String t = node.path("messageType").asText("");
        return t.isBlank() ? node.path("type").asText("") : t;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
