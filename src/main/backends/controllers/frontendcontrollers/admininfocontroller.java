package controllers.frontendcontrollers;

import Database.Inventory;
import Database.RequestLog;
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
import models.Extra.messages.Createitempayload;
import models.core.Account;
import models.core.Item;

import java.io.IOException;
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

    private Account adminAccount;

    public Consumer<String> user_requesthandler;

    private final RequestLog requestlog = new  RequestLog();


    @FXML
    public void initialize() {
        passshow.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
        setAdmin(UserSession.getCurrentAccount());

        inventory.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(
                            "Name: " + item.getName() + "\n" +
                                    "Price: " + item.getPrices() + "\n" +
                                    "Type: " + item.getType() + "\n" +
                                    "Desc: " + item.getInfo()
                    );
                }
            }
        });
        //=====================
        // load request từ requestlog
        loadrequest();
        subscribeuser_RequestResult();
        loadInventoryData();

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
    public void handle_start_auction(ActionEvent event) {
        showPlaceholderAlert();
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
                       String payloadjson =   node.get("payloadJson").asText();

                       Gson gson = new Gson();
                       Createitempayload payload = gson.fromJson(  payloadjson, Createitempayload.class);
                       item_wait_accepted.add(payload.getItemType());

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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thong bao");
        alert.setHeaderText(null);
        alert.setContentText("Chuc nang nay chua duoc cai dat.");
        alert.showAndWait();
    }
    private void loadrequest(){
        try{
            List<RequestLog.RequestRecord> requests = requestlog.getRequestsByType("additem");

            Gson gson = new Gson();
            for (RequestLog.RequestRecord request : requests) {
                Createitempayload payload = gson.fromJson(request.requestInfo(),Createitempayload.class);
                item_wait_accepted.add(payload.getItemType());
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
            System.out.println("admin dang xem thong tin san pham");
         });
         selected.setOnAction(event -> {
            System.out.println("admin will accept this item");
         });
     }
     @Override
     protected void updateItem(String item, boolean empty) {// javafx AUTO call it
         super.updateItem(item, empty);
         if (item!=null &&  !empty) {
             name_item.setText(item);
             setGraphic(content);
         }
         else
             setGraphic(null);
     }
}