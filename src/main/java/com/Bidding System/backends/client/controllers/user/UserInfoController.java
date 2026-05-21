package backends.client.controllers.user;

import backends.common.messages.MsgBid.CancelAutoBidding;
import backends.common.messages.MsgBid.RegisterAutoBidding;
import backends.server.database.MyRequestDAO;
import backends.server.database.RequestLogDAO;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import backends.client.controllers.ViewLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;
import javafx.stage.Stage;
import backends.common.messages.Common.Createitempayload;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.RemoveRequestpayload;
import backends.common.messages.MsgAuction.AuctionResultMessage;
import backends.common.messages.MsgAuction.AuctionStatusMessage;
import backends.common.messages.MsgAuction.StartAuctionMessage;
import backends.common.messages.MsgBid.ClientSendBid;
import backends.common.messages.MsgBid.ReceiveMaxBidder;
import backends.common.messages.MsgData.FetchDataRequest;
import backends.common.models.accounts.User;
import javafx.collections.ObservableList;
import backends.common.models.core.Item;
import backends.common.models.items.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class UserInfoController {
    @FXML
    private Label infoname;

    @FXML
    private Label infoemail;

    @FXML
    private Label infopassword;

    @FXML
    private Label infophonenumber;

    @FXML
    private Label lblTimer;

    @FXML
    private CheckBox passshow;

    @FXML
    private Button placebid;

    @FXML
    private Label balance;

    @FXML
    private TextField high_bidder;

    @FXML
    private TextField current_amount;

    @FXML
    private TextField bidprice;

    @FXML
    private TextField baseprice;

    @FXML
    private TextField increment;

    @FXML
    private TextField itemName;

    @FXML
    private Button autoBidButton;

    @FXML
    private TextField autoIncrement;

    @FXML
    private TextField maxLimit;

    private final MyRequestDAO myrequest = new MyRequestDAO();
    @FXML
    private ListView<String> List_AcceptedItem;

    private ObservableList<String> AcceptedItem_info = FXCollections.observableArrayList();
    private User user;

    @FXML
    private ListView<Item> ITEMLIST;
    private ObservableList<Item> upcomingAuctions = FXCollections.observableArrayList();

    private Consumer<String> depositResultHandler;
    private Consumer<String> change_infoResultHandler;
    private Consumer<String> AdditemResultHandler;
    private Consumer<String> auctionStartHandler;
    private Consumer<String> removeitemHandler;
    private Consumer<String> actionAcceptedHandler;
    private Consumer<String> auctionHandler;
    private volatile LocalDateTime endAt;
    private volatile String currentAuctionId;
    private Timeline timeline;
    private Timeline upcomingSyncTimeline;
    private static double currentBalance;
    private double startingPrice;
    private double currentBidIncrement;
    private double currentBiddingAmount;
    private String currentSellerId;
    private Item selectedAuctionItem;
    private boolean autoBidMode = false;
    private final java.util.Map<String, Long> currentEndTimeEpochs = new java.util.HashMap<>();
    private final java.util.Map<String, String> itemToAuctionId = new java.util.HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @FXML
    public void initialize() throws Exception {
        if (passshow != null) {
            passshow.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
        }
        if (UserSession.getCurrentUser() != null) {
            setUser(UserSession.getCurrentUser());
        }

        if (balance != null) {
            loadCurrentAmount();
            subscribeDepositResult();
        }

        if (ITEMLIST != null) {
            high_bidder.setEditable(false);
            current_amount.setEditable(false);
            itemName.setEditable(false);
            baseprice.setEditable(false);
            increment.setEditable(false);
            UserSession.getConnection().send(new FetchDataRequest("FETCH_INVENTORY"));
            loadupcomingAuctions();
            subcribePlaceBid();
            subscribeAuction();
            //subscribeAuctionStart();
            subscribeAuctionList();
            startUIUpdater();
        }

        if (List_AcceptedItem != null) {
            loaduser_request();
            remove_itemResult();
            subscribeAdditemResult();
            subscribeActionAccepted();
        }

        if (UserSession.getCurrentUser() != null) {
            System.out.println(UserSession.getCurrentUser().getId());
        }
    }

    private void loadCurrentAmount() {
        if (balance != null) {
            Platform.runLater(() -> balance.setText("0.0"));
        }
        Message get = new Message();
        get.messageType = "GET_BALANCE";
        get.Id_user = UserSession.getCurrentUser().getId();
        UserSession.getConnection().send(get);
    }

    public static double get_Balance() {
        return currentBalance;
    }


    private void loadupcomingAuctions() {

        ITEMLIST.setCellFactory(this::createUpcomingAuctionCell);
        ITEMLIST.setItems(upcomingAuctions);
        ITEMLIST.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldItem, selected) -> {
                    if (selected == null) {
                        return;
                    }
                    selectedAuctionItem = selected;
                    currentAuctionId = null;
                    currentSellerId = null;
                    currentBiddingAmount = 0;
                    endAt = null;
                    applyAuctionItemDetails(selected);
                    high_bidder.setText("Loading...");
                    current_amount.setText("Loading...");
                    placebid.setDisable(true);
                    bidprice.setDisable(true);
                    Long epoch = currentEndTimeEpochs.get(selected.getId());
                    String storedAuctionId = itemToAuctionId.get(selected.getId());
                    if (epoch != null && storedAuctionId != null) {
                        long remain = epoch - System.currentTimeMillis();
                        if (remain > 0) {
                            endAt = Instant.ofEpochMilli(epoch)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                            currentAuctionId = storedAuctionId; // restore ngay từ map
                            placebid.setDisable(false);         // enable bid ngay
                            bidprice.setDisable(false);
                            bidprice.clear();
                            high_bidder.setText("Loading...");  // vẫn loading, chờ server
                            current_amount.setText("Loading...");
                        } else {
                            endAt = null;
                            setClock0();
                        }
                    } else {
                        endAt = null;
                        setClock0();
                    }
                    // FETCH STATUS
                    ObjectNode req = new ObjectMapper().createObjectNode();
                    req.put("type", "FETCH_AUCTION_STATUS");
                    req.put("itemId", selected.getId());
                    UserSession.getConnection().send(req);
                });
    }

    private void subscribeAuctionList() {
        MessageBus.getInstance().subscribe(json -> {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = null;
            try {
                node = mapper.readTree(json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            String type = resolveMessageType(node);
            switch (type) {
                case "INVENTORY_DATA" -> {
                    List<Item> scheduled = parseItemsFromJson(node.path("scheduledItems"));
                    List<Item> inProgress = parseItemsFromJson(node.path("inProgressItems"));
                    List<Item> items = new ArrayList<>();
                    items.addAll(scheduled);
                    items.addAll(inProgress);

                    Platform.runLater(() -> upcomingAuctions.setAll(items));
                }
            }
        });
    }

    private ListCell<Item> createUpcomingAuctionCell(ListView<Item> listView) {
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
                                    "Opening: " + item.getPrices() + "\n" +
                                    "Type: " + item.getType() + "\n" +
                                    "Desc: " + item.getInfo()
                    );
                }
            }
        };
    }

    private void startUIUpdater() {
        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    try {
                        if (endAt == null) {
                            setClock0();
                        } else {
                            java.time.Duration remaining =
                                    java.time.Duration.between(LocalDateTime.now(), endAt);

                            if (remaining.isZero() || remaining.isNegative()) {
                                setClock0();
                            } else {
                                updateClock(remaining);
                            }
                        }

                    } catch (Exception e) {
                        System.err.println("Error updating UI: " + e.getMessage());
                    }
                })
        );

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void subcribePlaceBid() {
        MessageBus.getInstance().subscribe(json -> {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = null;
            try {
                node = mapper.readTree(json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            String type = resolveMessageType(node);
            switch (type) {
                case "RECEIVE_BID" -> {
                    ReceiveMaxBidder maxBidder_msg;
                    try {
                        maxBidder_msg = mapper.readValue(json, ReceiveMaxBidder.class);
                        String auctionId = maxBidder_msg.maxBidder.auctionId;
                        if(currentAuctionId == null || !currentAuctionId.equals(auctionId)) {
                            return;
                        }
                        Platform.runLater(() -> {
                            high_bidder.setText(String.valueOf(maxBidder_msg.maxBidder.name));
                            current_amount.setText(String.valueOf(maxBidder_msg.maxBidder.amount));
                            currentBiddingAmount = maxBidder_msg.maxBidder.amount;
                            placebid.setDisable(false);
                            bidprice.setDisable(false);
                            bidprice.clear();
                        });
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "BID_QUEUED" -> {
                    double queuedAmount = node.get("amount").asDouble();
                    Platform.runLater(() -> {
                        // Hiển thị thông báo nhỏ: bid đang chờ xử lý
                        bidprice.setStyle("-fx-border-color: orange;");
                        bidprice.setText("Your bid: " + queuedAmount + " is submitted");
                        placebid.setDisable(true);
                        bidprice.setDisable(true);
                    });
                }
                case "AUTO_BID_REGISTERED" -> {
                    placebid.setDisable(true);
                    maxLimit.setEditable(false);
                    autoIncrement.setEditable(false);
                    bidprice.setDisable(true);
                    autoBidButton.setText("STOP");
                    autoBidMode = true;
                    autoBidButton.setStyle("""
    -fx-background-color: #dc2626;
    -fx-text-fill: white;
    -fx-background-radius: 12;
    -fx-border-radius: 12;
    -fx-font: bold;
""");
                }
                case "AUTO_BID_CANCELLED" -> {
                    placebid.setDisable(false);
                    maxLimit.setEditable(true);
                    autoIncrement.setEditable(true);
                    bidprice.setDisable(false);
                    autoBidButton.setText("AUTO");
                    autoBidMode = false;
                    autoBidButton.setStyle("""
    -fx-background-color: #ea580c;
    -fx-text-fill: white;
    -fx-background-radius: 12;
    -fx-border-radius: 12;
""");
                }
            }
        });
    }
    private void subscribeAuctionStart() {
        auctionStartHandler = rawJson -> {
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            try {
                ObjectNode node = (ObjectNode) mapper.readTree(rawJson);
                String type = node.get("type").asText();
                Platform.runLater(() -> {
                    if (type.equals("START_AUCTION")) {
                        StartAuctionMessage msg = null;
                        try {
                            msg = mapper.readValue(rawJson, StartAuctionMessage.class);
                            baseprice.setText(Double.toString(msg.startingPrice));
                            increment.setText(Double.toString(msg.bidIncrement));
                            currentBidIncrement = msg.bidIncrement;
                            endAt = msg.endAt;
                            currentSellerId = msg.sellerId;
                            itemName.setText(msg.itemName);
                            currentAuctionId = msg.auctionId;
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        return;
                    }
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
        MessageBus.getInstance().subscribe(auctionStartHandler);
    }
    private void subscribeAuction() {
        auctionHandler = json -> {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node;
            try {
                node = mapper.readTree(json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            String type = resolveMessageType(node);
            switch (type) {
                case "AUCTION_STATUS" -> {
                    try {
                        AuctionStatusMessage statusMsg = mapper.readValue(json, AuctionStatusMessage.class);
                        Platform.runLater(() -> {
                            if ("STARTED".equals(statusMsg.status)) {
                                currentEndTimeEpochs.put(statusMsg.itemId, statusMsg.endTimeEpoch);
                                itemToAuctionId.put(statusMsg.itemId, statusMsg.auctionId);
                            } else {
                                currentEndTimeEpochs.remove(statusMsg.itemId);
                                itemToAuctionId.remove(statusMsg.itemId);
                            }
                            // Phần còn lại chỉ chạy nếu đúng item đang chọn
                            if (selectedAuctionItem == null || !selectedAuctionItem.getId().equals(statusMsg.itemId)) {
                                return;
                            }
                            if ("STARTED".equals(statusMsg.status)) {
                                currentAuctionId = statusMsg.auctionId;
                                currentSellerId = statusMsg.sellerId;
                                endAt = Instant.ofEpochMilli(statusMsg.endTimeEpoch)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime();
                                placebid.setDisable(false);
                                bidprice.setDisable(false);
                                Item runningItem = findItemById(statusMsg.itemId);
                                if (runningItem != null) {
                                    applyAuctionItemDetails(runningItem);
                                }
                                if (statusMsg.maxBidderName != null && !statusMsg.maxBidderName.isBlank()) {
                                    high_bidder.setText(statusMsg.maxBidderName);
                                    current_amount.setText(statusMsg.maxBidderAmount);
                                    try {
                                        currentBiddingAmount = Double.parseDouble(statusMsg.maxBidderAmount);

                                    } catch (Exception e) {
                                        currentBiddingAmount = startingPrice;
                                    }

                                }
                                else {
                                    high_bidder.setText("No bids yet");
                                    current_amount.setText(
                                            String.valueOf(startingPrice)
                                    );
                                    currentBiddingAmount = startingPrice;
                                }
                            } else if ("NOT_STARTED".equals(statusMsg.status)) {
                                // item scheduled nhưng chưa có phiên
                                endAt = null;
                                currentAuctionId = null;
                                setClock0();
                                high_bidder.setText("Not started yet");
                                current_amount.setText(String.valueOf(startingPrice));
                                placebid.setDisable(true);
                                bidprice.setDisable(true);
                            }
                            else if ("ENDED".equals(statusMsg.status)) {
                                currentEndTimeEpochs.remove(statusMsg.itemId);
                                currentAuctionId = null;
                                endAt = null;
                                setClock0();
                                high_bidder.setText("Auction ended");
                                current_amount.clear();
                                placebid.setDisable(true);
                                bidprice.setDisable(true);
                                UserSession.getConnection().send(new FetchDataRequest("FETCH_INVENTORY"));
                            }
                            else {
                                currentAuctionId = null;
                                endAt = null;
                                setClock0();
                                high_bidder.setText("No bids yet");
                                current_amount.setText(
                                        String.valueOf(startingPrice)
                                );
                                currentBiddingAmount = startingPrice;
                                placebid.setDisable(true);
                                bidprice.setDisable(true);
                            }
                        });
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "START_AUCTION" -> {
                    try {
                        ObjectMapper startMapper = new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                        StartAuctionMessage msg =
                                startMapper.readValue(json, StartAuctionMessage.class);
                        Platform.runLater(() -> {
                            // KHÔNG PHẢI ITEM ĐANG CHỌN
                            if (selectedAuctionItem == null ||
                                    !selectedAuctionItem.getId().equals(msg.auctionId)) {
                                return;
                            }
                            itemName.setText(msg.itemName);
                            baseprice.setText(String.valueOf(msg.startingPrice));
                            startingPrice = msg.startingPrice;
                            increment.setText(String.valueOf(msg.bidIncrement));
                            currentBidIncrement = msg.bidIncrement;
                            currentAuctionId = msg.auctionId;
                            currentSellerId = msg.sellerId;
                            if (msg.endAt != null) {
                                endAt = msg.endAt;
                            }
                            placebid.setDisable(false);
                            bidprice.setDisable(false);
                            if (high_bidder.getText().equals("Loading...")) {
                                high_bidder.setText("No bids yet");
                                current_amount.setText(String.valueOf(startingPrice));
                            }
                        });
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "AUCTION_RESULT" -> {
                    try {
                        AuctionResultMessage result = mapper.readValue(json, AuctionResultMessage.class);
                        Platform.runLater(() -> {
                            if (result.hasBidder) {
                                if (result.winnerId.equals(UserSession.getCurrentUser().getId())) {
                                    showAlert(
                                            Alert.AlertType.INFORMATION,
                                            "Auction Result",
                                            "Congratulation! You are the winner!"
                                                    + "\nAmount: " + result.winningAmount
                                                    + "\nItem name: " + result.itemName
                                                    + "\nYour balance was deducted automatically."
                                    );
                                    loadCurrentAmount();
                                } else {
                                    showAlert(
                                            Alert.AlertType.INFORMATION,
                                            "Auction Result",
                                            "Winner: " + result.winnerName
                                                    + "\nAmount: " + result.winningAmount
                                                    + "\nItem name: " + result.itemName
                                    );
                                }

                            } else {
                                showAlert(
                                        Alert.AlertType.INFORMATION,
                                        "Auction Result",
                                        "Auction ended without any bids"
                                );
                            }
                        });

                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        MessageBus.getInstance().subscribe(auctionHandler);
    }
    private void subscribeDepositResult() {
        depositResultHandler = rawJson -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(rawJson);
                String type = node.get("type").asText();

                if (type.equals("BALANCE_OK") && node.has("amount")) {
                    double latestBalance = node.get("amount").asDouble();
                    currentBalance = latestBalance;

                    User currentUser = UserSession.getCurrentUser();
                    if (currentUser != null) {
                        currentUser.setBalance(latestBalance);
                    }

                    if (balance != null) {
                        Platform.runLater(() -> balance.setText(String.valueOf(latestBalance)));
                    }
                    return;
                }

                if (type.equals("deposit_OK") && node.has("payloadJson")) {
                    String payloadjson = node.get("payloadJson").asText();
                    Gson gson = new Gson();
                    JsonNode payloadJsonNode = mapper.readTree(payloadjson);

                    double depositedAmount = payloadJsonNode.get("amount").asDouble();// lấy giá trị được gửi đến

                    User currentUser = UserSession.getCurrentUser();
                    if (currentUser == null) {
                        return;
                    }

                    double updatedBalance = depositedAmount;
                    currentBalance = updatedBalance;
                    currentUser.setBalance(updatedBalance);
                    if (balance != null) {
                        Platform.runLater(() -> balance.setText(String.valueOf(updatedBalance)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        MessageBus.getInstance().subscribe(depositResultHandler);
    }
    private void subscribeActionAccepted() {
        actionAcceptedHandler = rawJson -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode node = mapper.readTree(rawJson);
                String type = resolveMessageType(node);

                if (!"ACCEPTED_SUCCESS".equals(type)) {
                    return;
                }

                String requestId = node.path("request_id").asText("");
                String userId = node.path("user_id").asText("");
                String status = node.path("status").asText(MyRequestDAO.STATUS_WAITING);

                User currentUser = UserSession.getCurrentUser();
                if (currentUser == null || requestId.isBlank()) {
                    return;
                }

                if (!userId.isBlank() && !currentUser.getId().equals(userId)) {
                    return;
                }

                myrequest.updateRequestStatus(requestId, status);

                Platform.runLater(() -> {
                    try {
                        loaduser_request();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    showAlert(Alert.AlertType.INFORMATION, "Thong bao", "Item da duoc admin chap nhan");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        MessageBus.getInstance().subscribe(actionAcceptedHandler);
    }

    public void setUser(User user) {
        this.user = user;
        if (user == null) {
            return;
        }

        currentBalance = user.getBalance();
        if (infoname != null) {
            infoname.setText(user.getName());
        }
        if (infoemail != null) {
            infoemail.setText(user.getEmail());
        }
        if (infophonenumber != null) {
            infophonenumber.setText(user.getPhoneNumber());
        }
        if (balance != null) {
            balance.setText(String.valueOf(user.getBalance()));
        }
        refreshPasswordField();
    }

    @FXML
    public void handle_home(ActionEvent event) throws IOException {
        switchMainScene(event, "HomePage.fxml", "Home");
    }

    @FXML
    public void openProfile(ActionEvent event) throws IOException {
        switchMainScene(event, "UserInfo.fxml", "User Profile");
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
        cleanup();
        Parent root = ViewLoader.load(viewFileName);
        Scene scene = new Scene(root);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(scene);
        window.setTitle(title);
        fitToVisibleScreen(window);
        window.show();
    }

    @FXML
    public void handle_sign_out(ActionEvent event) throws IOException {
        UserSession.clear();
        cleanup();
        Parent root = ViewLoader.load("SignIn.fxml");
        Scene scene = new Scene(root);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        resetLoginWindowSize(window);
        window.setScene(scene);
        window.setTitle("Sign in");
        window.sizeToScene();
        window.centerOnScreen();
        window.show();
    }

    private void resetLoginWindowSize(Stage window) {
        window.setFullScreen(false);
        window.setMaximized(false);
        window.setMinWidth(0);
        window.setMinHeight(0);
        window.setWidth(450);
        window.setHeight(500);
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

    private void refreshPasswordField() {
        if (user == null || passshow == null || infopassword == null) {
            return;
        }
        if (!passshow.isSelected()) {
            infopassword.setText("*".repeat(user.getPassword().length()));
        } else {
            infopassword.setText(user.getPassword());
        }
    }
    private void remove_itemResult() throws Exception {
        removeitemHandler = rawJson -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode node = mapper.readTree(rawJson);
                String type = node.get("type").asText();
                if  (type.equals("remove_item_OK") && node.has("payloadJson")) {
                    String  payloadjson = node.get("payloadJson").asText();
                    Gson gson = new Gson();
                    RemoveRequestpayload payload = gson.fromJson(payloadjson ,  RemoveRequestpayload.class);
                    String request_id = payload.getRequest_id();
                    if (request_id == null || request_id.isBlank()) {
                        showAlert(Alert.AlertType.WARNING, "Loi", "Khong nhan duoc request_id hop le");
                        return;
                    }

                    myrequest.remove_request(request_id);
                    Platform.runLater(() -> {
                        AcceptedItem_info.remove(request_id);
                        AcceptedItem_info.clear();
                        try {
                            loaduser_request();// load lại request
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        showAlert(Alert.AlertType.INFORMATION,"ok", "remove item successfully");
                    });
                }
                else if( type.equals("remove_item_fail"))
                    showAlert(Alert.AlertType.WARNING , "khong thanh cong" , "item is now in auction or not but you cannot");

            } catch (IOException e) {

                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(removeitemHandler);
    }

    public void handleAutoBid(ActionEvent event) {
        String autoBidText = autoBidButton.getText();
        if (autoBidText.equals("AUTO")) {
            if (selectedAuctionItem == null) {
                new Alert(
                        Alert.AlertType.ERROR,
                        "Please select an auction item",
                        ButtonType.OK
                ).show();
                return;
            }
            if (endAt == null || currentAuctionId == null) {
                new Alert(
                        Alert.AlertType.ERROR,
                        "No auction is currently running",
                        ButtonType.OK
                ).show();
                return;
            }
            String incrementStr = autoIncrement.getText();
            String maxLimitStr = maxLimit.getText();
            double autoBidIncrement;
            double autoBidMaxLimit;
            try {
                autoBidIncrement = Double.parseDouble(incrementStr);
                autoBidMaxLimit = Double.parseDouble(maxLimitStr);
                if (autoBidIncrement <= 0) {
                    throw new IllegalArgumentException("increment must be positive");
                }
                if (autoBidMaxLimit <= 0) {
                    throw new IllegalArgumentException("max limit must be positive");
                }
                if (currentSellerId != null && currentSellerId.equals(UserSession.getCurrentUser().getId())) {
                    throw new IllegalArgumentException("You cannot bid on your own item");
                }
                if (autoBidMaxLimit > currentBalance) {
                    throw new IllegalArgumentException(
                            "Your balance is insufficient"
                    );
                }
                if(autoBidMaxLimit < currentBiddingAmount + currentBidIncrement) {
                    throw new IllegalArgumentException(
                            "Your max limit is insufficient"
                    );
                }
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Invalid number", ButtonType.OK).show();
                return;
            } catch (IllegalArgumentException e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).show();
                return;
            }
            java.time.Duration remaining =
                    java.time.Duration.between(LocalDateTime.now(), endAt);
            if (remaining.isZero() || remaining.isNegative()) {
                new Alert(
                        Alert.AlertType.ERROR,
                        "Auction expired",
                        ButtonType.OK
                ).show();
                return;
            }
            String userId = UserSession.getCurrentUser().getId();
            RegisterAutoBidding msg = new RegisterAutoBidding(currentAuctionId, userId, autoBidMaxLimit, autoBidIncrement);
            UserSession.getConnection().send(msg);
        }
        else {
            CancelAutoBidding msg = new CancelAutoBidding();
            msg.auctionId = currentAuctionId;
            msg.userId = UserSession.getCurrentUser().getId();
            UserSession.getConnection().send(msg);
        }
    }

    public void placebid(ActionEvent event) throws IOException {
        if (selectedAuctionItem == null) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "Please select an auction item",
                    ButtonType.OK
            ).show();
            return;
        }
        if (endAt == null || currentAuctionId == null) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "No auction is currently running",
                    ButtonType.OK
            ).show();
            return;
        }
        String amountStr = bidprice.getText();
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }
            if(autoBidMode) {
                throw new IllegalArgumentException("You can not bid while turning on auto bid");
            }
            if (currentSellerId != null && currentSellerId.equals(UserSession.getCurrentUser().getId())) {
                throw new IllegalArgumentException("You cannot bid on your own item");
            }
            if (amount < startingPrice + currentBidIncrement) {
                throw new IllegalArgumentException("Minimum bid: " + (startingPrice + currentBidIncrement));
            }
            if (amount > currentBalance) {
                throw new IllegalArgumentException(
                        "Your balance is insufficient"
                );
            }
            if (amount < currentBiddingAmount + currentBidIncrement) {
                throw new IllegalArgumentException(
                        "Minimum next bid: " + (currentBiddingAmount + currentBidIncrement)
                );
            }
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid number", ButtonType.OK).show();
            return;
        } catch (IllegalArgumentException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).show();
            return;
        }
        java.time.Duration remaining =
                java.time.Duration.between(LocalDateTime.now(), endAt);
        if (remaining.isZero() || remaining.isNegative()) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "Auction expired",
                    ButtonType.OK
            ).show();
            return;
        }
        UserSession.getConnection().send(new ClientSendBid(UserSession.getCurrentUser().getId(), amount, currentAuctionId));
    }

    private void setClock0() {
        if (lblTimer == null) {
            return;
        }
        lblTimer.setText("00:00:00");
        lblTimer.setTextFill(javafx.scene.paint.Color.RED);
    }

    private void updateClock(java.time.Duration remaining) {
        if (lblTimer == null) {
            return;
        }
        long h = remaining.toHours();
        long m = remaining.toMinutesPart();
        long s = remaining.toSecondsPart();
        lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
        lblTimer.setTextFill(javafx.scene.paint.Color.web("#fbbf24"));
    }

    private void applyAuctionItemDetails(Item item) {
        if (item == null) {
            return;
        }
        if (itemName != null) {
            itemName.setText(item.getName());
        }
        if (baseprice != null) {
            baseprice.setText(String.valueOf(item.getPrices()));
        }
        startingPrice = item.getPrices();
        if (increment != null) {
            increment.setText(String.valueOf(item.getBidIncrement()));
        }
        currentBidIncrement = item.getBidIncrement();
    }

    private Item findItemById(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        for (Item item : upcomingAuctions) {
            if (itemId.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    private String resolveMessageType(JsonNode node) {
        String messageType = node.path("messageType").asText("");
        if (!messageType.isBlank()) {
            return messageType;
        }
        return node.path("type").asText("");
    }

    public void autobid(ActionEvent event) throws IOException {
    }

    public void handle_deposit(ActionEvent event) throws IOException {
        FXMLLoader loader = ViewLoader.loader("Deposite.fxml");
        Parent root = loader.load();

        Scene sceneMain = new Scene(root);
        Stage window = new Stage();
        window.setScene(sceneMain);
        window.setTitle("DEPOSIT");
        window.centerOnScreen();
        window.show();
    }

    public void handle_create(ActionEvent event) throws IOException {
        FXMLLoader  loader = ViewLoader.loader("CreateItem.fxml");
        Parent root = loader.load();

        Scene sceneMain = new Scene(root);
        Stage window = new Stage();
        window.setScene(sceneMain);
        window.setTitle("CREATE");
        window.centerOnScreen();
        window.show();
    }
    // handle the save_change button
    public void subscribechangeResult(){
        change_infoResultHandler = rawJson -> {
            ObjectMapper mapper = new ObjectMapper();
            try{
                JsonNode node = mapper.readTree(rawJson);
                String type = node.get("type").asText();

//                JsonNode payloadJson = node.get("payloadJson");
                if(!type.equals("change_info_OK)")){
                    return;
                }

                Platform.runLater(() ->{
                    if (type.equals("change_info_OK") && node.has("payloadJson")) {
                        String payloadJson = node.get("payloadJson").asText();
                        Gson gson = new Gson();
//                        JsonNode payload = gson.fromJson(payloadJson, JsonNode.class);

//                        UserStore userstore = new UserStore();
//
//                        try {// server updates information right in database
//                            userstore.change_info(payload.get("new_name").asText(), payload.get("new_email").asText(), payload.get("new_phonenumber").asText(), payload.get("new_password").asText(), UserSession.getCurrentUser().getId());
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
                        showAlert(Alert.AlertType.INFORMATION, "thanh cong" , "change information sucessfull!");
                    }
                    else {
                        showAlert(Alert.AlertType.WARNING, "khong thanh cong" , "cannot change your information");
                    }
                });

            } catch (Exception e){
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(change_infoResultHandler);
    }
    public void cleanup() {
        if (depositResultHandler != null)
            MessageBus.getInstance().unsubscribe(depositResultHandler);
        if (change_infoResultHandler != null)
            MessageBus.getInstance().unsubscribe(change_infoResultHandler);
        if (AdditemResultHandler != null)
            MessageBus.getInstance().unsubscribe(AdditemResultHandler);
        if (auctionStartHandler != null)
            MessageBus.getInstance().unsubscribe(auctionStartHandler);
        if (removeitemHandler != null)
            MessageBus.getInstance().unsubscribe(removeitemHandler);
        if (actionAcceptedHandler != null)
            MessageBus.getInstance().unsubscribe(actionAcceptedHandler);
        if (auctionHandler != null)
            MessageBus.getInstance().unsubscribe(auctionHandler);
        if (timeline != null) timeline.stop();
    }

    public void subscribeAdditemResult(){
        AdditemResultHandler = rawJson -> {
            ObjectMapper mapper = new  ObjectMapper();
            try{
                JsonNode node = mapper.readTree(rawJson);
                String type =  node.get("type").asText();
                Platform.runLater(()->{
                    if (type.equals("add_item_OK") && node.has("payloadJson")) {
                        String payloadJson = node.get("payloadJson").asText();
                        Createitempayload payload =  new Gson().fromJson(payloadJson, Createitempayload.class);
                        Gson gson = new Gson();
                        String payloadjson = gson.toJson(payload);
                        Message msg = new Message();
                        msg.Id_user = UserSession.getCurrentUser().getId();
                        msg.messageType = "additem";
                        msg.payloadJson = payloadjson;
                        String requestId = node.has("request_id") ? node.get("request_id").asText() : null;

                        try {
                            myrequest.save_myrequest(msg, requestId);
                            if (requestId != null) {
                                AcceptedItem_info.add(requestId);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    List_AcceptedItem.setItems(AcceptedItem_info);// listview load item từ AcceptedItem_info
                    // set up cell factory -> để tạo ra 1 dòng chứa nhiều loại icon và button tương tác
                    List_AcceptedItem.setCellFactory(lv -> new CustomItemCell());
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(AdditemResultHandler);
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    public void loaduser_request() throws IOException {
        if (List_AcceptedItem == null) {
            return;
        }
        List<MyRequestDAO.RequestRecord> requests = myrequest.getMyRequestsByType("additem");

        AcceptedItem_info.clear();
        for (MyRequestDAO.RequestRecord request : requests) {
            User currentUser = UserSession.getCurrentUser();
            if (currentUser != null && !currentUser.getId().equals(request.userId())) {
                continue;
            }
            if (request.requestId() != null) {
                AcceptedItem_info.add(request.requestId());
            }
        }

        List_AcceptedItem.setItems(AcceptedItem_info);
        List_AcceptedItem.setCellFactory(lv -> new CustomItemCell());
    }
    private List<Item> parseItemsFromJson(JsonNode arrayNode) {
        List<Item> items = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) return items;

        for (JsonNode node : arrayNode) {
            try {
                String id = node.path("id").asText("");
                String typeStr = node.path("type").asText("");
                String name = node.path("name").asText("");

                double price = node.has("prices") ? node.path("prices").asDouble() : node.path("price").asDouble();
                double bidIncrement = node.has("bidIncrement")
                        ? node.path("bidIncrement").asDouble()
                        : node.path("bid_increment").asDouble(0);
                String info = node.path("info").asText("");

                ItemType itemType = ItemType.valueOf(typeStr);
                Item item = ItemFactory.createItem(itemType, name, price, info);
                item.setId(id);
                item.setBidIncrement(bidIncrement);

                items.add(item);
            } catch (Exception e) {
                System.err.println("Lỗi parse 1 item: " + e.getMessage());
            }
        }
        return items;
    }
}
// custom khung cho hiện thị ở list item ,
class CustomItemCell extends ListCell<String>{
    private HBox content;
    private Label name_item;
    private Button View_info;
    private Button remove_item;
    private Pane spacer;
    private final RequestLogDAO requestLogDAO = new RequestLogDAO();
    private final MyRequestDAO myrequest = new MyRequestDAO();
    private final Gson gson = new Gson();

    public CustomItemCell(){
        super();
        name_item = new Label();
        View_info = new Button("view");
        remove_item = new Button("remove");

        // spacer sẽ là khoản trắng để tạo khoảng cách cho các tác vụ
        // đưa 2 nút button về bên phải -> tách ra khỏi label name

        spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        content = new HBox(10, name_item , spacer , View_info, remove_item);
        content.setAlignment(Pos.CENTER_LEFT);

        View_info.setOnAction(event ->{
            String requestId = getItem();
            if (requestId == null) {
                return;
            }
            try {
                MyRequestDAO.RequestRecord request = myrequest.findByRequestId(requestId);
                if (request == null) {
                    return;
                }
                Createitempayload payload = gson.fromJson(request.requestInfo(), Createitempayload.class);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thong tin item");
                alert.setHeaderText(payload.getItem_name());
                alert.setContentText(
                        "Request ID: " + request.requestId() + "\n" +
                                "User ID: " + request.userId() + "\n" +
                                "Type: " + payload.getItemType() + "\n" +
                                "Base price: " + payload.getBasePrice() + "\n" +
                                "Increment: " + payload.getBidIncrement() + "\n" +
                                "Info: " + payload.getItemInfo() + "\n" +
                                "Status: " + request.status() + "\n" +
                                "Time: " + request.time()
                );
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        remove_item.setOnAction(event ->{
            String item = getItem();// trả về id_request

            Gson gson = new Gson();
            RemoveRequestpayload payload = new RemoveRequestpayload(item , myrequest.getStatusById(item));
            String payloadjson = gson.toJson(payload);

            Message msg = new Message();
            msg.Id_user = UserSession.getCurrentUser().getId();
            msg.messageType = "removeitem";
            msg.payloadJson = payloadjson;

            UserSession.getConnection().send(msg);
            System.out.println("đã xóa item khỏi history");
        });


    }
    @Override
    protected void updateItem(String item , boolean empty){// javafx AUTO call it
        super.updateItem(item, empty);
        if(item!=null && !empty){
            try {
                RequestLogDAO.RequestRecord request = requestLogDAO.findByRequestId(item);
                if (request != null) {
                    Createitempayload payload = gson.fromJson(request.requestInfo(), Createitempayload.class);
                    name_item.setText(payload.getItem_name());
                } else {
                    name_item.setText(item);
                }
            } catch (IOException e) {
                name_item.setText(item);
            }
            setGraphic(content);
        }
        else
            setGraphic(null);
    }
}
