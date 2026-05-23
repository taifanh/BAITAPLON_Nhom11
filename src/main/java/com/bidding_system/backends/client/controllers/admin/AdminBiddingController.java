package com.bidding_system.backends.client.controllers.admin;

import com.bidding_system.backends.client.controllers.base.BaseController;
import com.bidding_system.backends.client.controllers.components.BidHistoryRow;
import com.bidding_system.backends.client.controllers.components.CustomBidHistoryCell;
import com.bidding_system.backends.client.network.MessageBus;
import com.bidding_system.backends.client.session.UserSession;
import com.bidding_system.backends.client.controllers.util.ItemJsonParser;
import com.bidding_system.backends.common.messages.MsgAuction.AuctionCommandMessage;
import com.bidding_system.backends.common.messages.MsgAuction.AuctionStatusMessage;
import com.bidding_system.backends.common.messages.MsgAuction.StartAuctionMessage;
import com.bidding_system.backends.common.messages.MsgBid.ReceiveMaxBidder;
import com.bidding_system.backends.common.messages.MsgData.BidHistoryDataResponse;
import com.bidding_system.backends.common.messages.MsgData.BidHistoryRecordDto;
import com.bidding_system.backends.common.messages.MsgData.FetchBidHistoryRequest;
import com.bidding_system.backends.common.messages.MsgData.FetchDataRequest;
import com.bidding_system.backends.common.models.core.Item;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class AdminBiddingController extends BaseController {

    // ── FXML fields ───────────────────────────────────────────────
    @FXML private ListView<Item> upcomingItems;
    @FXML private Label          itemName;
    @FXML private TextField      basePrice;
    @FXML private TextField      bidIncrement;
    @FXML private TextField      setTime;
    @FXML private TextField      fieldHighBidder;
    @FXML private TextField      fieldCurrentAmount;
    @FXML private Label          errorStartAuction;
    @FXML private Button         buttonStartEndAuction;
    @FXML private Label          labelTimer;
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private TableView<BidHistoryRow> bidHistoryTable;

    // ── Constants ─────────────────────────────────────────────────
    private static final String MSG_INVENTORY     = "INVENTORY_DATA";
    private static final String MSG_AUCTION_STATUS = "AUCTION_STATUS";
    private static final String MSG_START_AUCTION  = "START_AUCTION";
    private static final String MSG_RECEIVE_BID    = "RECEIVE_BID";
    private static final String MSG_BID_HISTORY    = "BID_HISTORY_DATA";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ── Auction state ─────────────────────────────────────────────
    private final Set<String>          inProgressItemIds  = new HashSet<>();
    private final Map<String, Long>    endEpochByItemId   = new HashMap<>();
    private final Map<String, String>  auctionIdByItemId  = new HashMap<>();

    private Item            selectedItem;
    private String          currentAuctionId;
    private double          currentStartingPrice;
    private volatile boolean isRestoringSelection = false;

    private Timeline          countdownTimeline;
    private Consumer<String>  biddingHandler;
    private XYChart.Series<Number, Number> priceSeries;

    // ── Lifecycle ─────────────────────────────────────────────────
    @FXML
    public void initialize() {
        fieldHighBidder.setEditable(false);
        fieldCurrentAmount.setEditable(false);
        basePrice.setEditable(false);
        bidIncrement.setEditable(false);
        setupBidVisuals();

        upcomingItems.setCellFactory(lv -> createItemCell());
        upcomingItems.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    if (!isRestoringSelection && selected != null)
                        onItemSelected(selected);
                });

        subscribeMessages();
        startCountdownTimer();
    }


    public void cleanup() {
        if (biddingHandler != null)
            MessageBus.getInstance().unsubscribe(biddingHandler);
        if (countdownTimeline != null)
            countdownTimeline.stop();
    }

    // ── Item selection ────────────────────────────────────────────

    private void onItemSelected(Item item) {
        selectedItem     = item;
        currentAuctionId = null;

        fieldHighBidder.setText("Loading...");
        fieldCurrentAmount.setText("Loading...");
        clearBidVisuals("Select an active auction to view bid history.");
        applyItemDetails(item);

        if (inProgressItemIds.contains(item.getId())) {
            buttonStartEndAuction.setText("END AUCTION");
            setTime.setDisable(true);
        } else {
            buttonStartEndAuction.setText("START AUCTION");
            setTime.setDisable(false);
            resetClock();
            fieldHighBidder.setText("No bids yet");
            fieldCurrentAmount.setText("-");
            clearBidVisuals("Auction has not started.");
        }

        ObjectNode req = MAPPER.createObjectNode();
        req.put("type",   "FETCH_AUCTION_STATUS");
        req.put("itemId", item.getId());
        UserSession.getConnection().send(req);
    }

    // ── Button handlers ───────────────────────────────────────────

    @FXML
    public void handleStartEndAuction(ActionEvent event) {
        errorStartAuction.setText("");
        if (selectedItem == null) {
            errorStartAuction.setText("Please select an item");
            return;
        }
        if ("START AUCTION".equals(buttonStartEndAuction.getText())) {
            startAuction();
        } else {
            endAuction();
        }
    }

    private void startAuction() {
        String timeStr = setTime.getText().trim();
        if (timeStr.isEmpty()) {
            errorStartAuction.setText("Please enter auction duration (minutes)");
            return;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(timeStr);
        } catch (NumberFormatException e) {
            errorStartAuction.setText("Duration must be a whole number");
            return;
        }
        if (minutes <= 0) {
            errorStartAuction.setText("Duration must be > 0");
            return;
        }
        UserSession.getConnection()
                .send(new AuctionCommandMessage("START", selectedItem.getId(), minutes));
        buttonStartEndAuction.setDisable(true);
        setTime.clear();
    }

    private void endAuction() {
        UserSession.getConnection()
                .send(new AuctionCommandMessage("END", selectedItem.getId(), 0));
        buttonStartEndAuction.setDisable(true);
    }

    // ── MessageBus ────────────────────────────────────────────────

    private void subscribeMessages() {
        biddingHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                String   type = resolveType(node);
                switch (type) {
                    case MSG_INVENTORY      -> handleInventoryData(node);
                    case MSG_AUCTION_STATUS -> handleAuctionStatus(raw);
                    case MSG_START_AUCTION  -> handleStartAuction(raw);
                    case MSG_RECEIVE_BID    -> handleReceiveBid(raw);
                    case MSG_BID_HISTORY    -> handleBidHistoryData(raw);
                }
            } catch (Exception e) { e.printStackTrace(); }
        };
        MessageBus.getInstance().subscribe(biddingHandler);
    }

    private void handleInventoryData(JsonNode root) {
        List<Item> scheduled   = ItemJsonParser.parse(root.path("scheduledItems"));
        List<Item> inProgress  = ItemJsonParser.parse(root.path("inProgressItems"));

        List<Item> allUpcoming = new ArrayList<>(scheduled);
        allUpcoming.addAll(inProgress);

        Platform.runLater(() -> {
            String previousId = selectedItem != null ? selectedItem.getId() : null;

            inProgressItemIds.clear();
            inProgress.forEach(i -> inProgressItemIds.add(i.getId()));

            isRestoringSelection = true;
            try {
                upcomingItems.setItems(FXCollections.observableArrayList(allUpcoming));
                if (previousId != null) {
                    allUpcoming.stream()
                            .filter(i -> i.getId().equals(previousId))
                            .findFirst()
                            .ifPresent(upcomingItems.getSelectionModel()::select);
                }
            } finally {
                isRestoringSelection = false;
            }
        });
    }

    private void handleAuctionStatus(String raw) throws Exception {
        AuctionStatusMessage msg = MAPPER.readValue(raw, AuctionStatusMessage.class);
        Platform.runLater(() -> {
            buttonStartEndAuction.setDisable(false);
            switch (msg.status) {
                case "STARTED" -> {
                    inProgressItemIds.add(msg.itemId);
                    endEpochByItemId.put(msg.itemId, msg.endTimeEpoch);
                    auctionIdByItemId.put(msg.itemId, msg.auctionId);
                    if (isSelectedItem(msg.itemId)) applyStartedStatus(msg);
                }
                case "NOT_STARTED" -> {
                    if (isSelectedItem(msg.itemId)) applyNotStartedStatus();
                }
                case "ENDED" -> {
                    inProgressItemIds.remove(msg.itemId);
                    endEpochByItemId.remove(msg.itemId);
                    auctionIdByItemId.remove(msg.itemId);
                    if (isSelectedItem(msg.itemId)) applyEndedStatus();
                }
            }
        });
    }

    private void handleStartAuction(String raw) throws Exception {
        StartAuctionMessage msg = MAPPER.readValue(raw, StartAuctionMessage.class);
        Platform.runLater(() -> {
            if (selectedItem == null ||
                    !selectedItem.getId().equals(msg.auctionId)) return;
            itemName.setText(msg.itemName);
            basePrice.setText(String.valueOf(msg.startingPrice));
            bidIncrement.setText(String.valueOf(msg.bidIncrement));
        });
    }

    private void handleReceiveBid(String raw) throws Exception {
        ReceiveMaxBidder msg = MAPPER.readValue(raw, ReceiveMaxBidder.class);
        if (currentAuctionId == null ||
                !currentAuctionId.equals(msg.maxBidder.auctionId)) return;
        Platform.runLater(() -> {
            fieldHighBidder.setText(msg.maxBidder.name);
            fieldCurrentAmount.setText(String.valueOf(msg.maxBidder.amount));
            requestBidVisuals(currentAuctionId);
        });
    }

    // ── Status helpers ────────────────────────────────────────────

    private void applyStartedStatus(AuctionStatusMessage msg) {
        currentAuctionId = msg.auctionId;
        buttonStartEndAuction.setText("END AUCTION");
        setTime.setDisable(true);
        applyItemDetails(selectedItem);
        if (msg.maxBidderName != null && !msg.maxBidderName.isBlank()) {
            fieldHighBidder.setText(msg.maxBidderName);
            fieldCurrentAmount.setText(msg.maxBidderAmount);
        } else {
            fieldHighBidder.setText("No bids yet");
            fieldCurrentAmount.setText(String.valueOf(currentStartingPrice));
        }
        requestBidVisuals(currentAuctionId);
    }

    private void applyNotStartedStatus() {
        currentAuctionId = null;
        resetClock();
        buttonStartEndAuction.setText("START AUCTION");
        setTime.setDisable(false);
        fieldHighBidder.setText("Not started yet");
        fieldCurrentAmount.setText(String.valueOf(currentStartingPrice));
        clearBidVisuals("Auction has not started.");
    }

    private void applyEndedStatus() {
        currentAuctionId = null;
        resetClock();
        buttonStartEndAuction.setText("START AUCTION");
        setTime.setDisable(false);
        fieldHighBidder.setText("-");
        fieldCurrentAmount.setText("-");
        clearBidVisuals("Auction ended.");
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

    // ── Countdown timer ───────────────────────────────────────────

    private void startCountdownTimer() {
        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> tickCountdown()));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void tickCountdown() {
        if (selectedItem == null) return;
        Long epoch = endEpochByItemId.get(selectedItem.getId());
        if (epoch == null) return;

        long remaining = epoch - System.currentTimeMillis();
        if (remaining <= 0) { Platform.runLater(this::resetClock); return; }

        var duration = java.time.Duration.ofMillis(remaining);
        long h = duration.toHours(), m = duration.toMinutesPart(), s = duration.toSecondsPart();
        Platform.runLater(() -> {
            labelTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
            labelTimer.setTextFill(javafx.scene.paint.Color.web("#fbbf24"));
        });
    }

    private void resetClock() {
        labelTimer.setText("00:00:00");
        labelTimer.setTextFill(javafx.scene.paint.Color.RED);
    }

    // ── Utilities ─────────────────────────────────────────────────

    private boolean isSelectedItem(String itemId) {
        return selectedItem != null && selectedItem.getId().equals(itemId);
    }

    private void applyItemDetails(Item item) {
        if (item == null) return;
        itemName.setText(item.getName());
        basePrice.setText(String.valueOf(item.getPrices()));
        bidIncrement.setText(String.valueOf(item.getBidIncrement()));
        currentStartingPrice = item.getPrices();
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
