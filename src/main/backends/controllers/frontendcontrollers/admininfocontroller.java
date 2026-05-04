package controllers.frontendcontrollers;

import Database.Inventory;
import Database.RequestLog;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private ListView<RequestLog.RequestRecord> requestlist; // Đổi String thành RequestRecord

    private ObservableList<RequestLog.RequestRecord> item_wait_accepted = FXCollections.observableArrayList();

    public final java.util.Set<String> selectedRequestIds = new java.util.HashSet<>();
    private final java.util.Set<String> inProgressItemIds = new java.util.HashSet<>();

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

    private final java.util.Map<String, Long> currentEndTimeEpochs = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        passshow.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
        setAdmin(UserSession.getCurrentAccount());

        inventory.setCellFactory(this::createItemCell);
        upcomingitem.setCellFactory(this::createItemCell);

        requestlist.setCellFactory(ls -> new CustomItemrequestCell(selectedRequestIds));
        requestlist.setItems(item_wait_accepted); // Khóa cứng list vào giao diện

        loadrequest();
        subscribeuser_RequestResult();
        subcribeAuctionController();
        loadInventoryData();
        upcomingitem.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                handleAuctionClick(newValue);
            }
        });
        startUIUpdater();
    }

    private void subcribeAuctionController() {
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
                // Nhận danh sách Inventory mới nhất
                case "INVENTORY_DATA" -> {
                    try {
                        // Dùng JsonNode và hàm helper tự viết thay cho mapper.readValue
                        JsonNode rootNode = mapper.readTree(json);
                        List<Item> waitingItems = parseItemsFromJson(rootNode.path("waitingItems"));
                        List<Item> scheduledItems = parseItemsFromJson(rootNode.path("scheduledItems"));
                        List<Item> inProgressItems = parseItemsFromJson(rootNode.path("inProgressItems"));

                        Platform.runLater(() -> {
                            // BƯỚC 1: Ghi nhớ lại ID của các Item đang được click chọn
                            String selectedUpcomingId = (itemAuction != null) ? itemAuction.getId() : null;
                            String selectedInventoryId = (inventory.getSelectionModel().getSelectedItem() != null) ? inventory.getSelectionModel().getSelectedItem().getId() : null;

                            // BƯỚC 2: Cập nhật dữ liệu
                            inProgressItemIds.clear();
                            for (Item i : inProgressItems) {
                                inProgressItemIds.add(i.getId());
                            }

                            inventory.setItems(FXCollections.observableArrayList(waitingItems));

                            List<Item> upcoming = new ArrayList<>(scheduledItems);
                            upcoming.addAll(inProgressItems);
                            upcomingitem.setItems(FXCollections.observableArrayList(upcoming));

                            // BƯỚC 3: Trả lại con trỏ chuột về đúng Item cũ
                            if (selectedUpcomingId != null) {
                                for (Item i : upcomingitem.getItems()) {
                                    if (i.getId().equals(selectedUpcomingId)) {
                                        upcomingitem.getSelectionModel().select(i);
                                        break;
                                    }
                                }
                            }
                            if (selectedInventoryId != null) {
                                for (Item i : inventory.getItems()) {
                                    if (i.getId().equals(selectedInventoryId)) {
                                        inventory.getSelectionModel().select(i);
                                        break;
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("Lỗi parse INVENTORY_DATA: ");
                        e.printStackTrace();
                    }
                }

                // Nhận danh sách Request mới nhất
                case "REQUEST_LIST_DATA" -> {
                    try {
                        RequestListDataResponse resp = mapper.readValue(json, RequestListDataResponse.class);
                        Platform.runLater(() -> {
                            // Chỉ việc nhồi data mới vào, list sẽ TỰ ĐỘNG nảy số trên màn hình
                            // Tuyệt đối KHÔNG gọi lại setCellFactory hay setItems ở đây!
                            item_wait_accepted.setAll(resp.requests);
                        });
                    } catch (Exception e) {
                        System.err.println("Không the readValue của REQUEST_LIST_DATA của admincontroller");
                        e.printStackTrace();
                    }
                }

                // Nhận báo cáo thao tác thành công (ví dụ duyệt item xong)
                case "ACTION_SUCCESS" -> {
                    Platform.runLater(() -> {
                        loadInventoryData();
                        loadrequest();
                    });
                }
                case "AUCTION_STATUS" -> {
                    try {
                        AuctionStatusMessage statusMsg = mapper.readValue(json, AuctionStatusMessage.class);
                        Platform.runLater(() -> {
                            System.out.println("[Admin] Nhan duoc trang thai phien: " + statusMsg.status);
                            start_end_auction.setDisable(false);

                            if ("STARTED".equals(statusMsg.status)) {
                                inProgressItemIds.add(statusMsg.itemId);
                                currentEndTimeEpochs.put(statusMsg.itemId, statusMsg.endTimeEpoch);
                                Long epoch = statusMsg.endTimeEpoch - System.currentTimeMillis();
                                updateClock(java.time.Duration.ofMillis(epoch));
                            } else if ("ENDED".equals(statusMsg.status)) {
                                inProgressItemIds.remove(statusMsg.itemId);
                                currentEndTimeEpochs.remove(statusMsg.itemId);

                                if (itemAuction != null && itemAuction.getId().equals(statusMsg.itemId)) {
                                    settime.clear();
                                    clearUI();
                                }
                            }

                            loadInventoryData(); // Xin DB bản mới nhất

                            // Ép giao diện vẽ lại ngay lập tức
                            if (itemAuction != null && itemAuction.getId().equals(statusMsg.itemId)) {
                                for (Item i : upcomingitem.getItems()) {
                                    if (i.getId().equals(statusMsg.itemId)) {
                                        upcomingitem.getSelectionModel().select(i);
                                        handleAuctionClick(i);
                                        break;
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
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
        if (item == null) return;
        itemAuction = item;

        // Chỉ kiểm tra RAM nội bộ
        if (inProgressItemIds.contains(item.getId())) {
            // Phiên đang chạy
            start_end_auction.setText("END AUCTION");
            settime.setDisable(true);
            itemname.setText(item.getName());

            // Cơ chế Fallback: Nếu Map không có (do vừa login lại), lấy lại từ AuctionService
            Long epoch = currentEndTimeEpochs.get(item.getId());

            if (epoch == null || epoch == 0) {
                // Hỏi Server thay vì hỏi AuctionService local (vốn không có dữ liệu)
                lblTimer.setText("--:--:--");
                ObjectNode req = new ObjectMapper().createObjectNode();
                req.put("type", "FETCH_AUCTION_STATUS");
                req.put("itemId", item.getId());
                UserSession.getConnection().send(req.toString());
                // Khi server trả về AUCTION_STATUS với status=STARTED,
                // case "AUCTION_STATUS" sẽ tự cập nhật currentEndTimeEpochs
            }
        } else {
            // Phiên chưa chạy
            start_end_auction.setText("START AUCTION");
            settime.setDisable(false);
            itemname.setText(item.getName());
            lblTimer.setText("00:00:00");
            lblTimer.setTextFill(javafx.scene.paint.Color.RED);
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
        if (uiTimeline != null) {
            uiTimeline.stop(); // Tắt đồng hồ đếm ngược
        }
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
            UserSession.getConnection().send(new FetchDataRequest("FETCH_INVENTORY"));
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
        if (selectedRequestIds.isEmpty()) {
            showMessage("Thong bao", "Vui long chon request can tu choi.");
            return;
        }
        for (String reqId : selectedRequestIds) {
            UserSession.getConnection().send(new AdminActionCommand("REJECT_REQUEST", reqId));
        }
    }

    @FXML
    public void handle_accept_requests(ActionEvent event) {
        if (selectedRequestIds.isEmpty()) {
            showMessage("Thong bao", "Vui long chon request can duyet.");
            return;
        }
        for (String reqId : selectedRequestIds) {
            UserSession.getConnection().send(new AdminActionCommand("ACCEPT_REQUEST", reqId));
        }
    }

    @FXML
    public void handle_create_auction(ActionEvent event) {
        error_create_auction.setVisible(false);
        Item currentItem = inventory.getSelectionModel().getSelectedItem();
        if (currentItem == null) {
            error_create_auction.setVisible(true);
            return;
        }

        // Chỉ gửi lệnh lên Server
        UserSession.getConnection().send(new AdminActionCommand("SCHEDULE_ITEM", currentItem.getId()));
        inventory.getSelectionModel().clearSelection();
    }

    private Timeline uiTimeline;

    private void startUIUpdater() {
        if (uiTimeline != null) uiTimeline.stop();

        uiTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // Chỉ đếm ngược dựa vào cái biến Epoch đã lưu, không cần quan tâm DB hay RAM nữa
            if (itemAuction != null && currentEndTimeEpochs.containsKey(itemAuction.getId())) {
                long remainingMillis = currentEndTimeEpochs.get(itemAuction.getId()) - System.currentTimeMillis();

                if (remainingMillis <= 0) {
                    lblTimer.setText("00:00:00");
                    lblTimer.setTextFill(javafx.scene.paint.Color.RED);
                } else {
                    updateClock(java.time.Duration.ofMillis(remainingMillis));
                }
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

    private void clearUI() {
        lblTimer.setText("00:00:00");
        lblTimer.setTextFill(javafx.scene.paint.Color.RED);
        start_end_auction.setText("START AUCTION");
        settime.setDisable(false);
//        settime.clear();
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
            String timestr = settime.getText();
            if (timestr.equals("")) {
                error_start_auction.setText("Please enter a time");
                return;
            }
            int minutes;
            try {
                minutes = Integer.parseInt(timestr);
            } catch (NumberFormatException e) {
                error_start_auction.setText("Please enter an integer");
                return;
            }
            if (minutes <= 0) {
                error_start_auction.setText("Minutes must be > 0");
                return;
            }

            System.out.println("Gui yeu cau START AUCTION len Server...");
            UserSession.getConnection().send(new AuctionCommandMessage("START", itemAuction.getId(), minutes));

            start_end_auction.setDisable(true);
            settime.clear();

            upcomingitem.requestFocus();
        } else {
            System.out.println("Gui yeu cau END AUCTION (ep buoc) len Server...");
            // Tương tự, gửi lệnh END lên Server
            UserSession.getConnection().send(new AuctionCommandMessage("END", itemAuction.getId(), 0));
            start_end_auction.setDisable(true);
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
                    // Khi Server báo có 1 user vừa thêm request thành công
                    if (type.equals("add_item_OK") && node.has("payloadJson")){
                        System.out.println("[Admin] Co request moi tu User, dang tai lai danh sach...");
                        // Lập tức gọi hàm loadrequest() để xin Server danh sách mới nhất
                        loadrequest();
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
    private void loadrequest() {
        try {
            // Xóa danh sách các ô đã tick chọn trước đó để tránh lỗi dữ liệu
            selectedRequestIds.clear();
            // Xin Server danh sách Request mới nhất
            UserSession.getConnection().send(new FetchDataRequest("FETCH_REQUESTS"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private List<Item> parseItemsFromJson(JsonNode arrayNode) {
        List<Item> items = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) return items;

        for (JsonNode node : arrayNode) {
            try {
                String id = node.path("id").asText("");
                String typeStr = node.path("type").asText("");
                String name = node.path("name").asText("");
                // Đọc giá (phòng hờ json lưu 'price' hoặc 'prices')
                double price = node.has("prices") ? node.path("prices").asDouble() : node.path("price").asDouble();
                String info = node.path("info").asText("");

                ItemType itemType = ItemType.valueOf(typeStr);
                Item item = itemFactory.createItem(itemType, name, price, info);
                item.setId(id); // Set lại đúng ID từ Server

                items.add(item);
            } catch (Exception e) {
                System.err.println("Lỗi parse 1 item: " + e.getMessage());
            }
        }
        return items;
    }
}
class CustomItemrequestCell extends ListCell<RequestLog.RequestRecord> {
    private final HBox content;
    private final Button view;
    private final Label name_item;
    private final CheckBox selected;
    private final Gson gson = new Gson();
    private final java.util.Set<String> selectedIds; // Tham chiếu đến RAM của Controller

    protected CustomItemrequestCell(java.util.Set<String> selectedIds) {
        super();
        this.selectedIds = selectedIds;
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        name_item = new Label();
        selected = new CheckBox();
        view = new Button("view");

        content = new HBox(10, name_item, spacer, view, selected);
        content.setAlignment(Pos.CENTER_LEFT);

        view.setOnAction(event -> {
            RequestLog.RequestRecord request = getItem();
            if (request == null) return;

            Createitempayload payload = gson.fromJson(request.requestInfo(), Createitempayload.class);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thong tin item");
            alert.setHeaderText(payload.getItem_name());
            alert.setContentText(
                    "Request ID: " + request.id() + "\n" +
                            "User ID: " + request.userId() + "\n" +
                            "Type: " + payload.getItemType() + "\n" +
                            "Base price: " + payload.getBasePrice() + "\n" +
                            "Info: " + payload.getItemInfo()
            );
            alert.showAndWait();
        });

        selected.setOnAction(event -> {
            RequestLog.RequestRecord request = getItem();
            if (request != null) {
                if (selected.isSelected()) {
                    selectedIds.add(request.id()); // Lưu vào RAM
                } else {
                    selectedIds.remove(request.id());
                }
            }
        });
    }

    @Override
    protected void updateItem(RequestLog.RequestRecord request, boolean empty) {
        super.updateItem(request, empty);
        if (request != null && !empty) {
            Createitempayload payload = gson.fromJson(request.requestInfo(), Createitempayload.class);
            name_item.setText(payload.getItem_name());

            // Phục hồi trạng thái check dựa vào bộ nhớ RAM
            selected.setSelected(selectedIds.contains(request.id()));

            setGraphic(content);
        } else {
            setGraphic(null);
        }
    }
}