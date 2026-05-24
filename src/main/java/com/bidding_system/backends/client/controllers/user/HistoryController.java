package com.bidding_system.backends.client.controllers.user;

import com.bidding_system.backends.client.controllers.ViewLoader;
import com.bidding_system.backends.client.session.UserSession;
import com.bidding_system.backends.common.messages.Common.Createitempayload;
import com.bidding_system.backends.common.models.accounts.User;
import com.bidding_system.backends.server.database.BidTransactionDAO;
import com.bidding_system.backends.server.database.MyRequestDAO;
import com.google.gson.Gson;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class HistoryController {
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

    @FXML
    public void initialize() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        itemColumn.setCellValueFactory(new PropertyValueFactory<>("item"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        historyTable.setItems(rows);
        loadHistory();
    }

    @FXML
    public void backHome(ActionEvent event) throws IOException {
        switchMainScene(event, "HomePage.fxml", "Home");
    }

    @FXML
    public void openProfile(ActionEvent event) throws IOException {
        switchMainScene(event, "UserProfile.fxml", "User Profile");
    }

    @FXML
    public void openBiddingSpace(ActionEvent event) throws IOException {
        switchMainScene(event, "BiddingSpace.fxml", "Bidding Space");
    }

    @FXML
    public void openSellItem(ActionEvent event) throws IOException {
        switchMainScene(event, "SellItem.fxml", "Sell Item");
    }

    @FXML
    public void openHistory(ActionEvent event) throws IOException {
        switchMainScene(event, "History.fxml", "Transaction History");
    }

    private void switchMainScene(ActionEvent event, String viewFileName, String title) throws IOException {
        Parent root = ViewLoader.load(viewFileName);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.setTitle(title);
        fitToVisibleScreen(window);
        window.show();
    }

    private void fitToVisibleScreen(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setMaximized(false);
        stage.setMinWidth(1000);
        stage.setMinHeight(620);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    private void loadHistory() {
        User user = UserSession.getCurrentUser();
        if (user == null) {
            summaryLabel.setText("No user session found.");
            return;
        }

        rows.clear();
        loadSellRequests(user.getId());
        loadBidRows(user.getId());
        summaryLabel.setText(rows.size() + " transaction records");
    }

    private void loadSellRequests(String userId) {
        try {
            Gson gson = new Gson();
            MyRequestDAO myRequestDAO = new MyRequestDAO();
            for (MyRequestDAO.RequestRecord record : myRequestDAO.getMyRequestsByType("additem")) {
                if (!userId.equals(record.userId())) {
                    continue;
                }
                Createitempayload payload = gson.fromJson(record.requestInfo(), Createitempayload.class);
                String itemName = payload == null ? record.requestId() : payload.getItem_name();
                String amount = payload == null ? "" : String.valueOf(payload.getBasePrice());
                rows.add(new HistoryRow("Sell request", itemName, amount, record.status(), record.time()));
            }
        } catch (Exception e) {
            rows.add(new HistoryRow("Sell request", "Cannot load item history", "", "ERROR", ""));
        }
    }

    private void loadBidRows(String userId) {
        try {
            BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
            for (BidTransactionDAO.BidHistoryRecord record : bidTransactionDAO.getBidHistoryByBidder(userId)) {
                rows.add(new HistoryRow(
                        "Bid",
                        record.itemId(),
                        String.valueOf(record.amount()),
                        "PLACED",
                        record.bidTime().toString()
                ));
            }
        } catch (Exception e) {
            rows.add(new HistoryRow("Bid", "Cannot load bid history", "", "ERROR", ""));
        }
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
