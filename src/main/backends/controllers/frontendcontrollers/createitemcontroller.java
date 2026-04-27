package controllers.frontendcontrollers;

import Database.request_log;
import com.google.gson.Gson;
import controllers.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Extra.messages.Createitempayload;
import models.Extra.messages.Message;

import java.io.IOException;

public class createitemcontroller {
    @FXML
    public TextArea item_info;

    @FXML
    public TextField base_price;

    @FXML
    public TextField bid_increment;

    @FXML
    public ComboBox<String> item_type;

    private final request_log requestLog = new request_log();

    public void handle_create_ok(ActionEvent event) throws IOException {
        String type = item_type.getSelectionModel().getSelectedItem().toString();
        double bprice = Double.parseDouble(base_price.getText());
        double bincrement = Double.parseDouble(bid_increment.getText());
        String iteminfo = item_info.getText();

        Gson gson = new Gson();
        Createitempayload createitempayload = new Createitempayload(type, iteminfo, bprice, bincrement);
        String payload = gson.toJson(createitempayload);

        Message msg = new Message();
        msg.payloadJson = payload;
        msg.messageType = "additem";
        msg.Id_user = UserSession.getCurrentUser().getId();

        requestLog.save_request(msg);
        if (UserSession.getConnection() != null) {
            UserSession.getConnection().send(msg);
        }

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }

    @FXML
    public void initialize() {
        ObservableList<String> categories = FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"
        );
        item_type.setItems(categories);
    }

    public void handle_come_back(ActionEvent event) {
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }
}
