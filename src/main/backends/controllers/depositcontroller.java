package controllers;

import com.google.gson.Gson;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Extra.messages.Depositpayload;
import models.Extra.messages.Message;

import java.io.IOException;

public class depositcontroller {
    @FXML
    public TextField deposit_amount;
    @FXML
    public Button verifybtn;
    @FXML
    public TextField name_deposit_input;
    @FXML
    public TextField phonenumber_deposit_input;
    @FXML
    public Label money_display;

    public void ok_deposit(ActionEvent event){
        double money_in = Double.parseDouble(money_display.getText());

        Gson gson = new Gson();
        Depositpayload payload = new Depositpayload(money_in);
        String payloadJson = gson.toJson( payload); // convert the content into like   "{"amount" : 100.0}"

        Message msg = new Message();// create message of deposit
//        msg.id_user = "12345";
        msg.messageType = "deposit";
        msg.payloadJson = payloadJson;
        // this part for send request for server

        // the next part is for show screen


    }


}
