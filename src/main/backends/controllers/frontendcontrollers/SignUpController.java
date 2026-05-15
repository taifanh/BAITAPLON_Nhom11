package controllers.frontendcontrollers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import controllers.MessageBus;
import controllers.UserSession;
import controllers.ViewLoader;
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
import models.Extra.messages.Common.Message;
import models.Extra.messages.Common.SignupPayload;

import java.io.IOException;
import java.util.function.Consumer;

public class SignUpController {
    @FXML
    public TextField txtnameup;

    @FXML
    public TextField txtemailup;

    @FXML
    public PasswordField txtpassup;

    @FXML
    public TextField txtphonenumberup;

    @FXML
    public Button signup_ok;

    private Consumer<String> signupResultHandler;
    private Gson gson = new Gson();
    private final ObjectMapper mapper = new ObjectMapper();
    private Stage pendingStage;

    @FXML
    public void initialize() {
        receive_signup_ok();

        Platform.runLater(() -> {
            Stage stage = (Stage) signup_ok.getScene().getWindow();
            stage.setOnHidden(e -> cleanup());
        });
    }
    public void handle_signin(ActionEvent event) throws IOException {
        Parent signinRoot = ViewLoader.load("SignIn.fxml");
        Scene sceneSignin = new Scene(signinRoot);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(sceneSignin);
        window.setTitle("Sign in");
        window.centerOnScreen();
        window.show();
    }

    public void handle_signup_ok(ActionEvent event) {
        String name = txtnameup.getText() == null ? "" : txtnameup.getText().trim();
        String email = txtemailup.getText() == null ? "" : txtemailup.getText().trim();
        String phoneNumber = txtphonenumberup.getText() == null ? "" : txtphonenumberup.getText().trim();
        String password = txtpassup.getText() == null ? "" : txtpassup.getText().trim();

        if (name.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Loi", null, "Vui long nhap day du thong tin.");
            return;
        }
        // nếu đủ thông tin thì mới gửi tín hiệu cho server
        pendingStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        SignupPayload payload = new SignupPayload(name , email , phoneNumber , password);
        Message msg = new Message();
        msg.messageType = "signup";
        msg.payloadJson = gson.toJson(payload);
        UserSession.getConnection().send(msg);
    }

    public void receive_signup_ok(){
        signupResultHandler = rawJson ->{
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
        MessageBus.getInstance().subscribe(signupResultHandler);
    }
    private void cleanup() {
        if (signupResultHandler != null) {
            MessageBus.getInstance().unsubscribe(signupResultHandler);
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
