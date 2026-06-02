package backends.client.controllers.user;

import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import backends.client.controllers.base.BaseController;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.MessageType;
import backends.common.messages.Common.SignUpPayload;
import backends.client.controllers.ViewLoader;
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
    public static final double SIGNUP_WIDTH = 540;
    public static final double SIGNUP_HEIGHT = 590;

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
        signUpComplete.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            newScene.windowProperty().addListener((windowObs, oldWindow, newWindow) -> {
                if (newWindow instanceof Stage stage) {
                    stage.setOnHidden(e -> cleanup());
                }
            });
        });
    }
    public void handleSignIn(ActionEvent event) throws IOException {
        Parent signinRoot = ViewLoader.load("SignIn.fxml");
        Scene sceneSignin = new Scene(signinRoot, BaseController.LOGIN_WIDTH, BaseController.LOGIN_HEIGHT);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(sceneSignin);
        window.setTitle("Sign in");
        window.setWidth(BaseController.LOGIN_WIDTH);
        window.setHeight(BaseController.LOGIN_HEIGHT);
        window.centerOnScreen();
        window.show();
    }

    public void handleSuccessfulSignUp(ActionEvent event) {
        String name = signUpName.getText() == null ? "" : signUpName.getText().trim();
        String email = signUpEmail.getText() == null ? "" : signUpEmail.getText().trim();
        String phoneNumber = signUpPhoneNumber.getText() == null ? "" : signUpPhoneNumber.getText().trim();
        String password = signUpPassword.getText() == null ? "" : signUpPassword.getText().trim();

        if (name.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Error", null, "Please enter all the information !");
            return;
        }
        // nếu đủ thông tin thì mới gửi tín hiệu cho server
        pendingStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        SignUpPayload payload = new SignUpPayload(name , email , phoneNumber , password);
        Message msg = new Message();
        msg.messageType = MessageType.SIGN_UP.getValue();
        msg.payloadJson = gson.toJson(payload);
        UserSession.getConnection().send(msg);
    }

    public void receiveSuccessfulSignUp(){
        signUpResultHandler = rawJson ->{
            try{
                JsonNode node = mapper.readTree(rawJson);
                String type = node.path("type").asText("");

                if ( type.equals("SIGNUP_FAIL")){
                    showAlert(Alert.AlertType.WARNING, "Error", null, "Your phone number has been registered !");
                }
                else if (type.equals("SIGNUP_OK")){
                    Platform.runLater(() ->{
                        try {
                            if (pendingStage == null) {
                                pendingStage = (Stage) signUpComplete.getScene().getWindow();
                            }
                            if (pendingStage == null) {
                                showAlert(Alert.AlertType.ERROR, "Error", null, "Cannot find current window.");
                                return;
                            }

                            Parent signinRoot = ViewLoader.load("SignIn.fxml");
                            Scene sceneMain = new Scene(signinRoot, BaseController.LOGIN_WIDTH, BaseController.LOGIN_HEIGHT);

                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Successful !");
                            alert.setHeaderText(null);
                            alert.setContentText("Sign up successful !");
                            alert.setOnHidden(e -> {
                                pendingStage.setScene(sceneMain);
                                pendingStage.setTitle("Sign in");
                                pendingStage.setWidth(BaseController.LOGIN_WIDTH);
                                pendingStage.setHeight(BaseController.LOGIN_HEIGHT);
                                pendingStage.centerOnScreen();
                                pendingStage.show();
                            });
                            alert.show();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
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
