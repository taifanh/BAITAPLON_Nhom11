package com.bidding_system.backends.client.controllers.user;

import com.bidding_system.backends.client.controllers.base.BaseController;
import com.bidding_system.backends.client.network.MessageBus;
import com.bidding_system.backends.client.session.UserSession;
import com.bidding_system.backends.common.messages.Common.Createitempayload;
import com.bidding_system.backends.common.messages.Common.Message;
import com.bidding_system.backends.common.messages.MsgData.BidHistoryDataResponse;
import com.bidding_system.backends.common.messages.MsgData.BidHistoryRecordDto;
import com.bidding_system.backends.common.messages.MsgData.FetchBidHistoryRequest;
import com.bidding_system.backends.common.messages.MsgData.FetchUserRequestsRequest;
import com.bidding_system.backends.common.messages.MsgData.RequestRecordDto;
import com.bidding_system.backends.common.messages.MsgData.UserRequestListResponse;
import com.bidding_system.backends.common.models.accounts.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class HistoryController extends BaseController {
    private static final String MSG_USER_REQUESTS = "USER_REQUEST_LIST_DATA";
    private static final String MSG_USER_BID_HISTORY = "USER_BID_HISTORY_DATA";
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    private Label summaryLabel;

    @FXML
    private TableView<HistoryRow> historyTable;

    @FXML
    private TableColumn<HistoryRow, String> typeColumn;

    @FXML
    private TableColumn<HistoryRow, String> itemColumn;

    @FXML
    private TableColumn<HistoryRow, String> amountColumn;

    @FXML
    private TableColumn<HistoryRow, String> statusColumn;

    @FXML
    private TableColumn<HistoryRow, String> timeColumn;

    private final ObservableList<HistoryRow> rows = FXCollections.observableArrayList();
    private final ObservableList<HistoryRow> sellRows = FXCollections.observableArrayList();
    private final ObservableList<HistoryRow> bidRows = FXCollections.observableArrayList();

    private Consumer<String> historyHandler;

    @FXML
    public void initialize() throws IOException {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        itemColumn.setCellValueFactory(new PropertyValueFactory<>("item"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        historyTable.setItems(rows);
        subscribeHistoryMessages();
        loadHistory();
    }

    @Override
    public void cleanup() {
        if (historyHandler != null) {
            MessageBus.getInstance().unsubscribe(historyHandler);
        }
    }

    @FXML
    public void backHome(ActionEvent event) throws IOException {
        switchScene(event, "HomePage.fxml", "Home");
    }

    private void loadHistory() throws IOException {
        User user = UserSession.getCurrentUser();
        if (user == null) {
            summaryLabel.setText("No user session found.");
            return;
        }

        rows.clear();
        sellRows.clear();
        bidRows.clear();
        summaryLabel.setText("Loading transaction history...");
        requestSellRequests(user.getId());
        requestBidRows(user.getId());
    }

    private void subscribeHistoryMessages() {
        historyHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                String type = node.path("type").asText();
                if (MSG_USER_REQUESTS.equals(type)) {
                    handleSellRequests(raw);
                } else if (MSG_USER_BID_HISTORY.equals(type)) {
                    handleBidHistory(raw);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(historyHandler);
    }

    private void requestSellRequests(String userId) throws IOException {
        Message msg = new Message();
        msg.Id_user = userId;
        msg.messageType = "FETCH_USER_REQUEST";
        msg.payloadJson = MAPPER.writeValueAsString(new FetchUserRequestsRequest(userId, "additem"));
        UserSession.getConnection().send(msg);
    }

    private void requestBidRows(String userId) {
        UserSession.getConnection().send(FetchBidHistoryRequest.forBidder(userId));
    }

    private void handleSellRequests(String raw) throws IOException {
        UserRequestListResponse response = MAPPER.readValue(raw, UserRequestListResponse.class);
        ObservableList<HistoryRow> loadedRows = FXCollections.observableArrayList();
        Gson gson = new Gson();

        if (response.requests != null) {
            for (RequestRecordDto record : response.requests) {
                Createitempayload payload = gson.fromJson(record.requestInfo, Createitempayload.class);
                String itemName = payload == null ? record.requestId : payload.getItem_name();
                String amount = payload == null ? "" : String.valueOf(payload.getBasePrice());
                loadedRows.add(new HistoryRow("Sell request", itemName, amount, record.status, record.time));
            }
        }

        Platform.runLater(() -> {
            sellRows.setAll(loadedRows);
            refreshRows();
        });
    }

    private void handleBidHistory(String raw) throws IOException {
        BidHistoryDataResponse response = MAPPER.readValue(raw, BidHistoryDataResponse.class);
        ObservableList<HistoryRow> loadedRows = FXCollections.observableArrayList();

        if (response.records != null) {
            for (BidHistoryRecordDto record : response.records) {
                loadedRows.add(new HistoryRow(
                        "Bid",
                        record.itemId,
                        String.valueOf(record.amount),
                        "PLACED",
                        record.bidTime == null ? "" : record.bidTime.toString()
                ));
            }
        }

        Platform.runLater(() -> {
            bidRows.setAll(loadedRows);
            refreshRows();
        });
    }

    private void refreshRows() {
        rows.setAll(sellRows);
        rows.addAll(bidRows);
        summaryLabel.setText(rows.size() + " transaction records");
    }

    public static class HistoryRow {
        private final String type;
        private final String item;
        private final String amount;
        private final String status;
        private final String time;

        public HistoryRow(String type, String item, String amount, String status, String time) {
            this.type = type;
            this.item = item;
            this.amount = amount;
            this.status = status;
            this.time = time;
        }

        public String getType() {
            return type;
        }

        public String getItem() {
            return item;
        }

        public String getAmount() {
            return amount;
        }

        public String getStatus() {
            return status;
        }

        public String getTime() {
            return time;
        }
    }
}
