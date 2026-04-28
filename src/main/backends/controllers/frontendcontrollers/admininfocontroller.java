package controllers.frontendcontrollers;

import Database.Inventory;
import controllers.AuctionService;
import controllers.UserSession;
import controllers.ViewLoader;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.accounts.Admin;
import models.bidding.Auction;
import models.core.Account;
import models.core.Item;

import java.io.IOException;
import java.util.List;

public class admininfocontroller {
    @FXML
    private TextField infoname;

    @FXML
    private Label infoemail;

    @FXML
    private Label infopassword;

    @FXML
    private Label infophonenumber;

    @FXML
    private CheckBox passshow;

    @FXML
    private ListView<String> requestlist;

    @FXML
    private ListView<Item> inventory;

    @FXML
    private ListView<Item> upcomingitem;

    @FXML
    private Label itemname;

    @FXML
    private TextField baseprice;

    @FXML
    private TextField increment;

    @FXML
    private TextField changeincremt;

    @FXML
    private TextField settime;

    @FXML
    private Label error_create_auction;

    @FXML
    private Label error_start_auction;

    @FXML
    private Button start_end_auction;

    @FXML
    private Label lblTimer;

    private Account adminAccount;

    @FXML
    public void initialize() {
        passshow.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
        setAdmin(UserSession.getCurrentAccount());

        inventory.setCellFactory(this::createItemCell);
        upcomingitem.setCellFactory(this::createItemCell);

        loadInventoryData();
        startUIUpdater();
    }

    private ListCell<Item> createItemCell(ListView<Item> listView) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(
                            "Name: " + item.getName() + "\n" +
                                    "Price: " + item.getPrices() + "\n" +
                                    "Type: " + item.getType() + "\n" +
                                    "Desc: " + item.getInfo()
                    );
                }
            }
        };
    }

    public void setAdmin(Account account) {
        adminAccount = account;
        if (account == null) {
            return;
        }

        infoname.setText(account.getName());
        infoemail.setText(account.getEmail());
        infophonenumber.setText(account.getPhoneNumber());
        refreshPasswordField();
    }

    @FXML
    public void handle_sign_out(ActionEvent event) throws IOException {
        UserSession.clear();
        Parent root = ViewLoader.load("signin.fxml");
        Scene scene = new Scene(root);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(scene);
        window.setTitle("Sign in");
        window.centerOnScreen();
        window.show();
    }

    private void loadInventoryData() {
        try {
            Inventory inventoryDB = new Inventory();

            // Lấy các item WAITING
            List<Item> items = inventoryDB.getItemsByStatus(Inventory.STATUS_WAITING);
            inventory.setItems(FXCollections.observableArrayList(items));

            // Lấy các item IN_AUCTION
            List<Item> upcomingitems = inventoryDB.getItemsByStatus(Inventory.STATUS_IN_AUCTION);
            upcomingitem.setItems(FXCollections.observableArrayList(upcomingitems));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handle_reject_requests(ActionEvent event) {
        showPlaceholderAlert();
    }

    @FXML
    public void handle_accept_requests(ActionEvent event) {
        showPlaceholderAlert();
    }

    @FXML
    public void handle_create_auction(ActionEvent event) throws IOException {
        error_create_auction.setVisible(false);
        Item currentItem = inventory.getSelectionModel().getSelectedItem();
        if (currentItem == null) {
            error_create_auction.setVisible(true);
            return;
        }
        else {
            inventory.getSelectionModel().clearSelection();
        }

        Inventory inventoryDB = new Inventory();
        inventoryDB.updateItemStatus(currentItem.getId(), Inventory.STATUS_IN_AUCTION);

        List<Item> items = inventoryDB.getItemsByStatus(Inventory.STATUS_IN_AUCTION);
        upcomingitem.setItems(FXCollections.observableArrayList(items));

        loadInventoryData();
    }

    private void startUIUpdater() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            try {
                Item currentitem = upcomingitem.getSelectionModel().getSelectedItem();

                if (currentitem != null) {
                    Auction managedAuction = AuctionService.getManagedActiveAuction(currentitem.getId());
                    if (managedAuction != null) {
                        java.time.Duration remaining = AuctionService.getDuration(currentitem.getId());

                        if (remaining.isZero() || remaining.isNegative()) {
                            // Auction hết giờ - tự động end
                            try {
                                AuctionService.endAuction(managedAuction, java.time.LocalDateTime.now());
                                System.out.println("Auto-ending auction for item: " + currentitem.getId());
                            } catch (Exception e) {
                                System.err.println("Error auto-ending auction: " + e.getMessage());
                            }
                            lblTimer.setText("00:00:00");
                            lblTimer.setTextFill(javafx.scene.paint.Color.RED);
                            refreshUIState();
                        } else {
                            long h = remaining.toHours();
                            long m = remaining.toMinutesPart();
                            long s = remaining.toSecondsPart();
                            lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
                            lblTimer.setTextFill(javafx.scene.paint.Color.web("#fbbf24"));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating UI timer: " + e.getMessage());
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void refreshUIState() {
        try {
            upcomingitem.getSelectionModel().clearSelection();
            loadInventoryData();
            start_end_auction.setText("START AUCTION");
            settime.setDisable(false);
            settime.clear();
        } catch (Exception e) {
            System.err.println("Error refreshing UI state: " + e.getMessage());
        }
    }

    @FXML
    public void handle_start_auction(ActionEvent event) throws IOException {
        error_start_auction.setText("");

        Item currentItem = upcomingitem.getSelectionModel().getSelectedItem();
        if (currentItem == null) {
            error_start_auction.setText("Please select an item");
            return;
        }

        if (start_end_auction.getText().equals("START AUCTION")) {
            String timestr = settime.getText();
            if (timestr.equals("")) {
                error_start_auction.setText("Please enter a time");
                return;
            }
            int minutes;
            try {
                minutes = Integer.parseInt(timestr);
            } catch (NumberFormatException e) {
                error_start_auction.setText("Please enter a integer minutes");
                return;
            }

            if (minutes < 0) {
                error_start_auction.setText("Please minutes greater than 0");
                return;
            }

            try {
                System.out.println("Starting auction for item: " + currentItem.getId());
                Auction currentAuction = AuctionService.startAuction((Admin) UserSession.getCurrentAccount(), currentItem, 0, minutes, 0);
                System.out.println("Auction started successfully. Auction ID: " + currentAuction.getAuctionId());
                start_end_auction.setText("END AUCTION");
                settime.setDisable(true);
            } catch (Exception e) {
                System.err.println("Error starting auction: " + e.getMessage());
                e.printStackTrace();
                error_start_auction.setText("Lỗi: " + e.getMessage());
            }
        }
        else {
            try {
                System.out.println("Ending auction for item: " + currentItem.getId());
                Auction currentAuction = AuctionService.getManagedActiveAuction(currentItem.getId());
                AuctionService.endAuction(currentAuction, java.time.LocalDateTime.now());

                System.out.println("Auction ended successfully");
                lblTimer.setText("00:00:00");
                lblTimer.setTextFill(javafx.scene.paint.Color.RED);
                start_end_auction.setText("START AUCTION");
                settime.setDisable(false);
                settime.clear();
                loadInventoryData();
            } catch (Exception e) {
                System.err.println("Error ending auction: " + e.getMessage());
                e.printStackTrace();
                error_start_auction.setText("Lỗi: " + e.getMessage());
            }
        }
    }

    @FXML
    public void autobid(ActionEvent event) {
        showPlaceholderAlert();
    }

    @FXML
    public void placebid(ActionEvent event) {
        showPlaceholderAlert();
    }

    private void refreshPasswordField() {
        if (adminAccount == null) {
            return;
        }

        if (passshow.isSelected()) {
            infopassword.setText(adminAccount.getPassword());
        } else {
            infopassword.setText("*".repeat(adminAccount.getPassword().length()));
        }
    }

    private void showPlaceholderAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thong bao");
        alert.setHeaderText(null);
        alert.setContentText("Chuc nang nay chua duoc cai dat.");
        alert.showAndWait();
    }
}
