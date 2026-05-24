package com.bidding_system.backends.client.controllers.user;

import com.bidding_system.backends.client.network.MessageBus;
import com.bidding_system.backends.client.session.UserSession;
import com.bidding_system.backends.common.messages.Common.Message;
import com.bidding_system.backends.common.messages.Common.MessageType;
import com.bidding_system.backends.common.messages.Common.SignupPayload;
import com.bidding_system.backends.client.controllers.ViewLoader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class SignUpController {
    @FXML
    public TextField signUpName;

    @FXML
    public TextField signUpEmail;

    @FXML
    public PasswordField signUpPassword;

    @FXML
    public TextField signUpPhoneNumber;

    @FXML
    public Button signUpComplete;

    private Consumer<String> signUpResultHandler;
    private Gson gson = new Gson();
    private final ObjectMapper mapper = new ObjectMapper();
    private Stage pendingStage;

    @FXML
    public void initialize() {
        receiveSuccessfulSignUp();

        Platform.runLater(() -> {
            Stage stage = (Stage) signUpComplete.getScene().getWindow();
            stage.setOnHidden(e -> cleanup());
        });
    }
    public void handleSignIn(ActionEvent event) throws IOException {
        Parent signinRoot = ViewLoader.load("SignIn.fxml");
        Scene sceneSignin = new Scene(signinRoot);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(sceneSignin);
        window.setTitle("Sign in");
        window.centerOnScreen();
        window.show();
    }

    public void handleSuccessfulSignUp(ActionEvent event) {
        String name = signUpName.getText() == null ? "" : signUpName.getText().trim();
        String email = signUpEmail.getText() == null ? "" : signUpEmail.getText().trim();
        String phoneNumber = signUpPhoneNumber.getText() == null ? "" : signUpPhoneNumber.getText().trim();
        String password = signUpPassword.getText() == null ? "" : signUpPassword.getText().trim();

        if (name.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Loi", null, "Vui long nhap day du thong tin.");
            return;
        }
        // nếu đủ thông tin thì mới gửi tín hiệu cho server
        pendingStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        SignupPayload payload = new SignupPayload(name , email , phoneNumber , password);
        Message msg = new Message();
        msg.messageType = MessageType.SIGNUP.getValue();
        msg.payloadJson = gson.toJson(payload);
        UserSession.getConnection().send(msg);
    }

    public void receiveSuccessfulSignUp(){
        signUpResultHandler = rawJson ->{
            try{
                JsonNode node = mapper.readTree(rawJson);
                String type = node.path("type").asText("");

                if ( type.equals("SIGNUP_FAIL")){
                    showAlert(Alert.AlertType.WARNING, "Trung du lieu", null, "So dien thoai da ton tai.");
                }
                else if (type.equals("SIGNUP_OK")){
                    Parent signinRoot = ViewLoader.load("SignIn.fxml");
                    Scene sceneMain = new Scene(signinRoot);

                    Platform.runLater(() ->{
                        showAlert(Alert.AlertType.INFORMATION, "THÀNH CÔNG", null, "sign up thành công.");

                        pendingStage.setScene(sceneMain);
                        pendingStage.setTitle("Sign in");
                        pendingStage.centerOnScreen();
                        pendingStage.show();
                    });
                }
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        MessageBus.getInstance().subscribe(signUpResultHandler);
    }
    private void cleanup() {
        if (signUpResultHandler != null) {
            MessageBus.getInstance().unsubscribe(signUpResultHandler);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
