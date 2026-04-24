package controllers.frontendcontrollers;

import com.google.gson.Gson;
import controllers.UserSession;
import controllers.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Extra.messages.Message;
import models.Extra.messages.placeBidpayload;
import models.accounts.User;

import java.io.IOException;

public class userinfocontroller {
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
    private Button placebid;

    @FXML
    private Label balance;

    @FXML
    private Button autobid;

    private User user;

    @FXML
    public void initialize() {
        passshow.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
        if (UserSession.getCurrentUser() != null) {
            setUser(UserSession.getCurrentUser());
        }
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
    // chua dung
    public void placebid(ActionEvent event ) throws IOException{
//        double money_bid = Double.parseDouble(money_display.getText());
//
//        Gson gson = new Gson();
//        placeBidpayload payload = new placeBidpayload(money_bid);
//        String payloadJson = gson.toJson( payload); // convert the content into like   "{"amount" : 100.0}"
//
//        Message msg = new Message();// create message of deposit
////        msg.id_user = "12345";
//        msg.messageType = "bid";
//        msg.payloadJson = payloadJson;
//        // this part still need checking for possible bid(compare max and increase)
//
//        // code here //
    }

    public void autobid(ActionEvent event ) throws IOException{}

    public void handle_deposit(ActionEvent event) throws IOException {
        FXMLLoader loader = ViewLoader.loader("deposite.fxml");
        Parent root = loader.load();

        Scene sceneMain = new Scene(root);
        Stage window = new  Stage();
        window.setScene(sceneMain);
        window.setTitle("DEPOSIT");
        window.centerOnScreen();
        window.show();
    }
    public void handle_create(ActionEvent event) throws IOException {}
}
