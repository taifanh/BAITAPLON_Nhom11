package controllers.frontendcontrollers;

import Database.RequestLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import controllers.MessageBus;
import controllers.ServerConnection;
import controllers.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import models.Extra.messages.Createitempayload;
import models.Extra.messages.Message;
import javafx.event.ActionEvent;
import java.awt.*;

import java.io.IOException;
import java.util.function.Consumer;


public class createitemcontroller {
    @FXML
    public  TextArea item_info;

    @FXML
    public TextField base_price;

    @FXML
    public TextField bid_increment;

    @FXML
    public ComboBox<String> item_type;

    @FXML
    public TextField item_name;

    private Consumer<String> createitemHandler;

    private final RequestLog requestLog = new RequestLog();

    public void handle_create_ok(ActionEvent event) throws IOException {

        String type = item_type.getSelectionModel().getSelectedItem().toString();
        double bprice = Double.parseDouble(base_price.getText());
        double bincrement = Double.parseDouble(bid_increment.getText());
        String iteminfo = item_info.getText();
        String itemname = item_name.getText();
// this part for handle send request to server database with request
        Gson gson = new Gson();
        Createitempayload createitempayload = new  Createitempayload(type,itemname,iteminfo,bprice,bincrement);
        String payload = gson.toJson(createitempayload);

        Message msg = new Message();
        msg.payloadJson = payload;
        msg.messageType = "additem";
        msg.Id_user = UserSession.getCurrentUser().getId();

        UserSession.getConnection().send(msg);// server gửi cho clienthandler

        // this part for handle showing window
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();

    }
    @FXML
    public void initialize() {
        // 1. Tạo danh sách các loại sản phẩm
        ObservableList<String> categories = FXCollections.observableArrayList(
                "Electronics" , "Art" , "Vehicle"
        );

        // 2. Đổ danh sách này vào ComboBox
        item_type.setItems(categories);

        // ... Giữ nguyên các code cũ của bạn như passhide.addListener ...
        subscribeCreateResult();
        Platform.runLater(()->{
            Stage stage = (Stage) item_info.getScene().getWindow();
            stage.setOnHidden(e -> cleanup());
        });
    }

    public void handle_come_back(ActionEvent event) throws IOException {
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }

    public void subscribeCreateResult(){
        createitemHandler = rawJson -> {
            ObjectMapper mapper = new ObjectMapper();
            try{
                ObjectNode node = (ObjectNode) mapper.readTree(rawJson);
                String type = node.get("type").asText();

                Platform.runLater(()->{
                    if (type.equals("OK")) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "tao san pham thanh cong!");
                        closeWindow();
                    }
                    else
                        showAlert(Alert.AlertType.WARNING, "Khong thanh cong" , "khong the tao san pham");

                });

            } catch (JsonMappingException e) {
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }
    public void cleanup() {
        if (createitemHandler != null) {
            MessageBus.getInstance().unsubscribe(createitemHandler);
        }
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    private void closeWindow() {
        Stage stage = (Stage) item_type.getScene().getWindow();
        stage.close();
    }

}
