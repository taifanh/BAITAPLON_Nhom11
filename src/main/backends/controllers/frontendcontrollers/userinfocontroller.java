package controllers.frontendcontrollers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import controllers.MessageBus;
import controllers.UserSession;
import controllers.ViewLoader;
import javafx.application.Platform;
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
import models.accounts.User;

import java.io.IOException;
import java.util.function.Consumer;

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

    private Consumer<String> depositResultHandler;

    @FXML
    public void initialize() {
        passshow.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
        if (UserSession.getCurrentUser() != null) {
            setUser(UserSession.getCurrentUser());
        }
        subscribeDepositResult();
    }

    private void subscribeDepositResult() {
        depositResultHandler = rawJson -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(rawJson);
                String type = node.get("type").asText();

                if (type.equals("OK") && node.has("payloadJson")) {
                    String payloadjson = node.get("payloadJson").asText();
                    Gson gson = new Gson();
                    JsonNode payloadJsonNode = mapper.readTree(payloadjson);

                    double depositedAmount = payloadJsonNode.get("amount").asDouble();

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
        FXMLLoader loader = ViewLoader.loader("create.fxml");
    }
}
