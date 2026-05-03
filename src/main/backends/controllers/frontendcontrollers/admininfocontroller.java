package controllers.frontendcontrollers;

import Database.Inventory;
import Database.RequestLog;
import controllers.AuctionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import controllers.MessageBus;
import controllers.UserSession;
import controllers.ViewLoader;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Extra.messages.*;
import models.accounts.Admin;
import models.bidding.Auction;
import models.core.Account;
import models.core.Item;
import models.items.ItemType;
import models.items.itemFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    private TextField high_bidder;

    @FXML
    private TextField current_amount;

    @FXML
    private ListView<String> requestlist;

    private ObservableList<String> item_wait_accepted = FXCollections.observableArrayList();

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

    @FXML
    private ListView<Item> runningitem;

    private Account adminAccount;

    public Consumer<String> user_requesthandler;

    private final RequestLog requestlog = new  RequestLog();

    private Item itemAuction = null;

    @FXML
    public void initialize() {
        passshow.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
        setAdmin(UserSession.getCurrentAccount());

        inventory.setCellFactory(this::createItemCell);
        upcomingitem.setCellFactory(this::createItemCell);

        loadrequest();
        subscribeuser_RequestResult();
        subcribePlaceBid();
        loadInventoryData();
        upcomingitem.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleAuctionClick(newValue);
            }
        });
        startUIUpdater();
    }

    private void subcribePlaceBid() {
        MessageBus.getInstance().subscribe(json -> {
            System.out.println("[AdminInfoController] Received message: " + json);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = null;
            try {
                node = mapper.readTree(json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            String type = resolveMessageType(node);
            System.out.println("[AdminInfoController] Message type: " + type);
            switch (type) {
                case "RECEIVE_BID" -> {
                    ReceiveMaxBidder maxBidder_msg;
                    try {
                        maxBidder_msg = mapper.readValue(json, ReceiveMaxBidder.class);
                        Platform.runLater(() -> {
                            high_bidder.setText(String.valueOf(maxBidder_msg.maxBidder.name));
                            current_amount.setText(String.valueOf(maxBidder_msg.maxBidder.amount));
                        });
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        });
    }

    private String resolveMessageType(JsonNode node) {
        String messageType = node.path("messageType").asText("");
        if (!messageType.isBlank()) {
            return messageType;
        }
        return node.path("type").asText("");
    }

    private void handleAuctionClick(Item item) {
        itemAuction = item;
        Auction currentAuction = AuctionService.getManagedActiveAuction(itemAuction.getId());

        Inventory inventoryDB;
        try {
            inventoryDB = new Inventory();
            if (Inventory.STATUS_IN_PROGRESS.equals(inventoryDB.getStatusById(item.getId()))) {
                if (currentAuction != null) {
                    java.time.Duration remaining = AuctionService.getDuration(currentAuction.getItem().getId());
                    updateClock(remaining);
                } else {
                    lblTimer.setText("00:00:00");
                    lblTimer.setTextFill(javafx.scene.paint.Color.RED);
                }
                start_end_auction.setText("END AUCTION");
                settime.setDisable(true);
                itemname.setText(item.getName());
            } else {
                start_end_auction.setText("START AUCTION");
                settime.setDisable(false);
                itemname.setText(item.getName());
                lblTimer.setText("00:00:00");
                lblTimer.setTextFill(javafx.scene.paint.Color.RED);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

            // Save old selections
            Item selectedInventory = inventory.getSelectionModel().getSelectedItem();
            Item selectedUpcoming = upcomingitem.getSelectionModel().getSelectedItem();

            // Lấy các item WAITING
            List<Item> items = inventoryDB.getItemsByStatus(Inventory.STATUS_WAITING);

            // Chỉ cập nhật UI nếu có sự thay đổi (giảm tải lag freeze)
            inventory.setItems(FXCollections.observableArrayList(items));

            List<Item> scheduledItems = inventoryDB.getItemsByStatus(Inventory.STATUS_SCHEDULED);
            List<Item> inProgressItems = inventoryDB.getItemsByStatus(Inventory.STATUS_IN_PROGRESS);
            List<Item> allItems = new ArrayList<>(scheduledItems);
            allItems.addAll(inProgressItems);

            upcomingitem.setItems(FXCollections.observableArrayList(allItems));

            // Restore selections silently without triggering aggressive UI updates
            if (selectedInventory != null) {
                for (Item i : items) {
                    if (i.getId().equals(selectedInventory.getId())) {
                        inventory.getSelectionModel().select(i);
                        break;
                    }
                }
            }
            if (selectedUpcoming != null) {
                for (Item i : allItems) {
                    if (i.getId().equals(selectedUpcoming.getId())) {
                        upcomingitem.getSelectionModel().select(i);
                        itemAuction = upcomingitem.getSelectionModel().getSelectedItem();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private  void loadRequestList(){
        try{
            RequestLog requestLogDB = new RequestLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handle_reject_requests(ActionEvent event) {
        String requestId = requestlist.getSelectionModel().getSelectedItem();// lấy các item không được tích chọn
        if (requestId == null || requestId.isBlank()) {
            showMessage("Thong bao", "Vui long chon request can tu choi.");
            return;
        }

        try {
            requestlog.updateRequestStatus(requestId, RequestLog.STATUS_REJECTED);
            item_wait_accepted.remove(requestId);
            requestlist.getSelectionModel().clearSelection();
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Loi", "Khong the tu choi request.");
        }
    }

    @FXML
    public void handle_accept_requests(ActionEvent event) throws IOException {
        Inventory inventoryDB = new Inventory();// item và userid
        List<RequestLog.RequestRecord> selected_requests = requestlog.selected_requests();
        Gson gson = new Gson();

        for (RequestLog.RequestRecord request : selected_requests) {
            String payload = request.requestInfo();
            String userId = request.userId();

            Createitempayload createitempayload = gson.fromJson(payload, Createitempayload.class);
            ItemType itemType = ItemType.valueOf(createitempayload.getItemType());
            Item item = itemFactory.createItem(
                    itemType,
                    createitempayload.getItem_name(),
                    createitempayload.getBasePrice(),
                    createitempayload.getItemInfo()
            );

            inventoryDB.saveItem(item, userId);
            // xóa request khỏi request_list -> chuyển sang inventory

            requestlog.updateRequestStatus(request.id(), RequestLog.STATUS_ACCEPTED);

            item_wait_accepted.remove(request.id());
        }
        item_wait_accepted.clear();
        loadrequest();
        loadInventoryData();
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
        inventoryDB.updateItemStatus(currentItem.getId(), Inventory.STATUS_SCHEDULED);

        List<Item> items = inventoryDB.getItemsByStatus(Inventory.STATUS_SCHEDULED);
        upcomingitem.setItems(FXCollections.observableArrayList(items));

        loadInventoryData();
    }
    private void sendListItem() {
        try {
            Inventory inventoryDB = new Inventory();
            List<Item> scheduledItems = inventoryDB.getItemsByStatus(Inventory.STATUS_SCHEDULED);
            List<Item> inProgressItems = inventoryDB.getItemsByStatus(Inventory.STATUS_IN_PROGRESS);
            List<Item> items = new ArrayList<>(scheduledItems);
            items.addAll(inProgressItems);

            List<AuctionItemDto> upcomingitems = items.stream()
                    .map(i -> new AuctionItemDto(
                            i.getId(),
                            i.getType(),
                            i.getName(),
                            i.getPrices(),
                            i.getInfo()
                    ))
                    .toList();

            UserSession.getConnection().send(new AuctionItemsResponse(upcomingitems));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Timeline uiTimeline; // Khai báo biến toàn cục trong class

    private void startUIUpdater() {
        if (uiTimeline != null) uiTimeline.stop();

        uiTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            try {
                // 1. Lấy danh sách tất cả item từ ListView (hoặc từ Database/Service)
                List<Item> allItems = new ArrayList<>(upcomingitem.getItems());

                boolean needRefresh = false;

                for (Item item : allItems) {
                    // Kiểm tra từng item xem có phiên đấu giá nào đang chạy không
                    Auction auction = AuctionService.getManagedActiveAuction(item.getId());

                    if (auction != null) {
                        java.time.Duration remaining = AuctionService.getDuration(item.getId());

                        // 2. Nếu bất kỳ phiên nào hết giờ, thực hiện kết thúc ngay
                        if (remaining.isZero() || remaining.isNegative()) {
                            System.out.println("Auto ending auction for item: " + item.getName());

                            // Gọi logic kết thúc (giống như khi bấm nút End Auction)
                            AuctionService.endAuction(auction, java.time.LocalDateTime.now());

                            // Gửi message báo cho các client khác (nếu cần)
//                            UserSession.getConnection().send(new EndAuctionMessage(...));

                            needRefresh = true;
                        }
                    }
                }

                // 3. Cập nhật UI cho item đang được chọn (nếu có)
                if (itemAuction != null) {
                    Auction currentAuction = AuctionService.getManagedActiveAuction(itemAuction.getId());
                    if (currentAuction != null) {
                        updateClock(AuctionService.getDuration(itemAuction.getId()));
                    } else {
                        cleartUI();
                    }
                }

                // 4. Nếu có item vừa kết thúc, làm mới danh sách
                if (needRefresh) {
                    refreshUIState();
                }

            } catch (Exception e) {
                System.err.println("Error in background update: " + e.getMessage());
            }
        }));
        uiTimeline.setCycleCount(Animation.INDEFINITE);
        uiTimeline.play();
    }

    private void refreshUIState() {
        try {
//            upcomingitem.getSelectionModel().clearSelection();
            loadInventoryData();
        } catch (Exception e) {
            System.err.println("Error refreshing UI state: " + e.getMessage());
        }
    }

    private void cleartUI() {
        lblTimer.setText("00:00:00");
        lblTimer.setTextFill(javafx.scene.paint.Color.RED);
        start_end_auction.setText("START AUCTION");
        settime.setDisable(false);
        settime.clear();
    }

    private void updateClock(java.time.Duration remaining) {
        long h = remaining.toHours();
        long m = remaining.toMinutesPart();
        long s = remaining.toSecondsPart();
        lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
        lblTimer.setTextFill(javafx.scene.paint.Color.web("#fbbf24"));
    }

    @FXML
    public void handle_start_auction(ActionEvent event) throws IOException {
        error_start_auction.setText("");

        if (itemAuction == null) {
            error_start_auction.setText("Please select an item");
            return;
        }

        if (start_end_auction.getText().equals("START AUCTION")) {
            if (itemAuction == null) {
                error_start_auction.setText("Please select an item");
                return;
            }
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
                System.out.println("Starting auction for item: " + itemAuction.getId());
                Auction currentAuction = AuctionService.startAuction((Admin) UserSession.getCurrentAccount(), itemAuction, 0, minutes, 0);
                System.out.println("about to send");
                UserSession.getConnection().send(
                        new StartAuctionMessage(
                                currentAuction.getEndAt(),
                                currentAuction.getItem().getName(),
                                currentAuction.getAuctionId(),
                                currentAuction.getItem().getPrices(),
                                0
                        )
                );
                System.out.println("sent");
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
                System.out.println("Ending auction for item: " + itemAuction.getId());
                Auction currentAuction = AuctionService.getManagedActiveAuction(itemAuction.getId());

                if (currentAuction == null) {
                    System.out.println("Cảnh báo: Dữ liệu phiên đấu bị lỗi (Orphan). Đang tiến hành reset trạng thái item về WAITING...");
                    Inventory inventoryDB = new Inventory();
                    inventoryDB.updateItemStatus(itemAuction.getId(), Inventory.STATUS_WAITING);
                    cleartUI();
                    refreshUIState();
                    return;
                }

//                UserSession.getConnection().send(
//                        new EndAuctionMessage(
//                                currentAuction.getEndAt(),
//                                currentAuction.getItem().getName(),
//                                currentAuction.getAuctionId(),
//                                currentAuction.getItem().getPrices(),
//                                0
//                        )
//                );

                AuctionService.endAuction(currentAuction, java.time.LocalDateTime.now());

                System.out.println("Auction ended successfully");
                cleartUI();
                refreshUIState();
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
    public void subscribeuser_RequestResult(){
        user_requesthandler = rawJson -> {
            ObjectMapper mapper = new ObjectMapper();
            try{
                JsonNode node = mapper.readTree(rawJson);
                String type = node.get("type").asText();
                Platform.runLater(() -> {
                    if (type.equals("add_item_OK") && node.has("payloadJson")){
                        String requestId = node.path("request_id").asText("");
                        if (requestId.isBlank() || item_wait_accepted.contains(requestId)) {
                            return;
                        }
                        item_wait_accepted.add(requestId);

                        requestlist.setItems(item_wait_accepted);
                        requestlist.setCellFactory(ls -> new CustomItemrequestCell() );
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(user_requesthandler);
    }

    private void showPlaceholderAlert() {
        showMessage("Thong bao", "Chuc nang nay chua duoc cai dat.");
    }

    private void showMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void loadrequest(){
        try{
            item_wait_accepted.clear();
            List<RequestLog.RequestRecord> requests = requestlog.getRequestsByType("additem");

            for (RequestLog.RequestRecord request : requests) {
                item_wait_accepted.add(request.id());
            }
            requestlist.setItems(item_wait_accepted);
            requestlist.setCellFactory(ls -> new CustomItemrequestCell());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
class CustomItemrequestCell  extends  ListCell<String> {
    private HBox content;
    private Button view;
    private Label name_item;
    private CheckBox selected;
    private Pane spacer;
    private final RequestLog requestLog = new RequestLog();
    private final Gson gson = new Gson();

    protected CustomItemrequestCell(){
        super();
        spacer = new Pane();
        HBox.setHgrow(spacer , Priority.ALWAYS);

        name_item = new Label();
        selected = new CheckBox();
        view = new  Button("view");

        content  = new HBox(10 , name_item , spacer, view , selected );
        content.setAlignment(Pos.CENTER_LEFT);

        view.setOnAction(event -> {
            String requestId = getItem();
            if (requestId == null) {
                return;
            }
            try {
                RequestLog.RequestRecord request = requestLog.findByRequestId(requestId);
                if (request == null) {
                    return;
                }
                Createitempayload payload = gson.fromJson(request.requestInfo(), Createitempayload.class);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thong tin item");
                alert.setHeaderText(payload.getItem_name());
                alert.setContentText(
                        "Request ID: " + request.id() + "\n" +
                                "User ID: " + request.userId() + "\n" +
                                "Type: " + payload.getItemType() + "\n" +
                                "Base price: " + payload.getBasePrice() + "\n" +
                                "Increment: " + payload.getBidIncrement() + "\n" +
                                "Info: " + payload.getItemInfo() + "\n" +
                                "Time: " + request.time()
                );
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        selected.setOnAction(event -> {
            if (getListView() != null) {
                getListView().getSelectionModel().select(getItem());
                requestLog.set_selected_request(getItem(),selected.isSelected());
            }
        });
    }
    @Override
    protected void updateItem(String item, boolean empty) {// javafx AUTO call it
        super.updateItem(item, empty);
        if (item!=null &&  !empty) {
            try {
                RequestLog.RequestRecord request = requestLog.findByRequestId(item);
                if (request != null) {
                    Createitempayload payload = gson.fromJson(request.requestInfo(), Createitempayload.class);
                    name_item.setText(payload.getItem_name());
                    selected.setSelected(request.selected());
                } else {
                    name_item.setText(item);
                    selected.setSelected(false);
                }
            } catch (IOException e) {
                name_item.setText(item);
                selected.setSelected(false);
            }
            setGraphic(content);
        }
        else
            setGraphic(null);
    }
}
