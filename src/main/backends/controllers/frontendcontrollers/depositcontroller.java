package controllers.frontendcontrollers;

import Database.request_log;
import com.google.gson.Gson;
import controllers.ServerConnection;
import controllers.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Extra.messages.Depositpayload;
import models.Extra.messages.Message;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class depositcontroller {
    @FXML
    public TextField deposit_amount;

    @FXML
    public TextField name_deposit_input;
    @FXML
    public TextField phonenumber_deposit_input;
    @FXML
    public Label money_display;


    public void ok_deposit(ActionEvent event) throws IOException {
        double money_in =  Double.parseDouble(money_display.getText());

        Gson gson = new Gson();
        Depositpayload payload = new Depositpayload(money_in);
        String payloadJson = gson.toJson( payload); // convert the content into like   "{"amount" : 100.0}"

        Message msg = new Message();// create message of deposit
        msg.Id_user = UserSession.getCurrentUser().getId();
        msg.messageType = "deposit";
        msg.payloadJson = payloadJson;
        // this part for send request for server
        // client send request then  server  receive and save into database

        ServerConnection.send(msg);

        // the next part is for show screen
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }

    public void handle_verify_money(ActionEvent event){
        money_display.setText(deposit_amount.getText());

    }



}
