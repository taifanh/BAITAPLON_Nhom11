package controllers.frontendcontrollers;

import Database.UserStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import controllers.MessageBus;
import controllers.UserSession;
import controllers.ViewLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import models.Extra.messages.ClientSendBid;
import models.Extra.messages.ReceiveMaxBidder;
import models.Extra.messages.ServerBidRespond;
import models.Extra.messages.Createitempayload;
import models.Extra.messages.Message;
import models.Extra.messages.placeBidpayload;
import models.accounts.User;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

import java.io.IOException;
import java.util.function.Consumer;

public class userinfocontroller {
    @FXML
    private Label infoname;

    @FXML
    private Label infoemail;

    @FXML
    private Label infopassword;

    @FXML
    private Label infophonenumber;

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
    private Button autobid;

    @FXML
    private ListView<String> List_AcceptedItem;

    private ObservableList<String> AcceptedItem_info = FXCollections.observableArrayList();
    private User user;

    private Consumer<String> depositResultHandler;
    private Consumer<String> change_infoResultHandler;
    private Consumer<String> AdditemResultHandler;


    @FXML
    public void initialize() {
        passshow.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
        if (UserSession.getCurrentUser() != null) {
            setUser(UserSession.getCurrentUser());
        }
        subscribeDepositResult();
        subcribePlaceBid();
        subscribeAdditemResult();
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

    private void subscribeDepositResult() {
        depositResultHandler = rawJson -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(rawJson);
                String type = node.get("type").asText();

                if (type.equals("deposit_OK") && node.has("payloadJson")) {
                    String payloadjson = node.get("payloadJson").asText();
                    Gson gson = new Gson();
                    JsonNode payloadJsonNode = mapper.readTree(payloadjson);

                    double depositedAmount = payloadJsonNode.get("amount").asDouble();// lấy giá trị được gửi đến

                    User currentUser = UserSession.getCurrentUser();
                    if (currentUser == null) {
                        return;
                    }

                    double updatedBalance = currentUser.getBalance() + depositedAmount;
                    currentUser.setBalance(updatedBalance);
                    Platform.runLater(() -> balance.setText(String.valueOf(updatedBalance)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        MessageBus.getInstance().subscribe(depositResultHandler);
    }

    public void setUser(User user) {
        this.user = user;
        if (user == null) {
            return;
        }

        infoname.setText(user.getName());
        infoemail.setText(user.getEmail());
        infophonenumber.setText(user.getPhoneNumber());
        balance.setText(String.valueOf(user.getBalance()));
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

    private void refreshPasswordField() {
        if (user == null) {
            return;
        }
        if (!passshow.isSelected()) {
            infopassword.setText("*".repeat(user.getPassword().length()));
        } else {
            infopassword.setText(user.getPassword());
        }
    }
    public void placebid(ActionEvent event) throws IOException {
        String amountStr = bidprice.getText();
        Double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid amount", ButtonType.OK).show();
            return;
        }
        String currentUserId = UserSession.getCurrentUser().getId();
        String auctionId = "USER_ROOM_" + currentUserId;
        UserSession.getConnection().send(new ClientSendBid(currentUserId, amount, auctionId));
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
        FXMLLoader loader = ViewLoader.loader("deposite.fxml");
        Parent root = loader.load();

        Scene sceneMain = new Scene(root);
        Stage window = new Stage();
        window.setScene(sceneMain);
        window.setTitle("DEPOSIT");
        window.centerOnScreen();
        window.show();
    }

    public void handle_create(ActionEvent event) throws IOException {
        FXMLLoader  loader = ViewLoader.loader("createitem.fxml");
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
                    else
                        showAlert(Alert.AlertType.WARNING, "khong thanh cong" , "cannot change your information");

                });

            } catch (Exception e){
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(change_infoResultHandler);
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
                       AcceptedItem_info.add(payload.getItemType());// cập nhật danh sachs -> thêm 1 item mới

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
}
// custom khung cho hiện thị ở list item ,
class CustomItemCell extends ListCell<String>{
    private HBox content;
    private Label name_item;
    private Button View_info;
    private Button remove_item;
    private Pane spacer;

    public CustomItemCell(){
        super();
        name_item = new Label();
        View_info = new Button("view");
        remove_item = new Button("remove");

        // spacer sẽ là khoản trắng để tạo khoảng cách cho các tác vụ
        // đưa 2 nút button về bên phải -> tách ra khỏi label name

        spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        content = new HBox(10, name_item ,  View_info, remove_item);
        content.setAlignment(Pos.CENTER_LEFT);

        View_info.setOnAction(event ->{
            String item = getItem();
            // tạo giao diện cho phần hiển thị thông tin
            // tạm thời in terminal
           System.out.println("đang xem thông tin của item : ");
        });
        remove_item.setOnAction(event ->{
            String item = getItem();
            getListView().getItems().remove(item);
           System.out.println("đã xóa item khỏi history");
        });


    }
    @Override
    protected void updateItem(String item , boolean empty){
        super.updateItem(item, empty);
        if(item!=null && !empty){
            name_item.setText(item);
            setGraphic(content);
        }
        else
            setGraphic(null);
    }
}

