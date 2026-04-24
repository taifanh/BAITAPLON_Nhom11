package controllers.frontendcontrollers;

import Database.request_log;
import com.google.gson.Gson;
import controllers.ServerConnection;
import controllers.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import models.Extra.messages.Createitempayload;
import models.Extra.messages.Message;
import javafx.event.ActionEvent;
import java.awt.*;

import java.io.IOException;


public class createitemcontroller {
    @FXML
    public  TextArea item_info;

    @FXML
    public TextField base_price;

    @FXML
    public TextField bid_increment;

    @FXML
    public ComboBox<String> item_type;

    private final request_log request_log = new request_log() ;

    public void handle_create_ok(ActionEvent event) throws IOException {

        String type = item_type.getSelectionModel().getSelectedItem().toString();
          double bprice = Double.parseDouble(base_price.getText());
          double bincrement = Double.parseDouble(bid_increment.getText());
          String iteminfo = item_info.getText();
// this part for handle send request to server database with request
          Gson gson = new Gson();
          Createitempayload createitempayload = new  Createitempayload(iteminfo,bprice,bincrement);
          String payload = gson.toJson(createitempayload);

          Message msg = new Message();
          msg.payloadJson = payload;
          msg.messageType = "additem";
          msg.Id_user = UserSession.getCurrentUser().getId();

          ServerConnection.send(msg);// server gửi cho clienthandler

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
    }

    public void handle_come_back(ActionEvent event) throws IOException {
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }

}
