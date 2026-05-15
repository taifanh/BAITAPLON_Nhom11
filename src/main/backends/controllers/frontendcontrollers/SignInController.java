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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Extra.messages.Common.Message;
import models.Extra.messages.Common.SigninPayload;
import models.Extra.messages.Common.SigninResponsePayload;
import models.accounts.Admin;
import models.accounts.User;
import models.core.Account;

import java.io.IOException;
import java.util.function.Consumer;

public class SignInController {
    @FXML
    public TextField txtphonenumberfield;

    @FXML
    public PasswordField txtpassfield;

    @FXML
    public Button signinbtn;

    @FXML
    public Button signupbtn;


    private final Gson gson = new Gson();
    private final ObjectMapper mapper = new ObjectMapper();
    private Consumer<String> signinResultHandler;
    private Stage pendingStage;

    @FXML
    public void initialize() {
        receive_signin_ok();

        Platform.runLater(() -> {
            Stage stage = (Stage) txtphonenumberfield.getScene().getWindow();
            stage.setOnHidden(e -> cleanup());
        });
    }

    public void handle_signin(ActionEvent event) {
        String phoneNumber = txtphonenumberfield.getText() == null ? "" : txtphonenumberfield.getText().trim();
        String password = txtpassfield.getText() == null ? "" : txtpassfield.getText().trim();

        if (phoneNumber.isBlank() || password.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Loi", null, "Vui long nhap day du so dien thoai va mat khau.");
            return;
        }
        // lấy stage hiên tại để thay đổi mà hình khi sign in thành công
        pendingStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Message msg = new Message();
        msg.messageType = "signin";
        msg.payloadJson = gson.toJson(new SigninPayload(phoneNumber, password));

        UserSession.getConnection().send(msg);// còn tín hiệu gửi login cũ thì sẽ cho client xử lý luôn nếu đăng nhập thành công
    }
    private void receive_signin_ok() {
        signinResultHandler = rawJson -> {
            try {
                JsonNode node = mapper.readTree(rawJson);
                String type = node.path("type").asText("");

                if ("SIGNIN_FAIL".equals(type)) {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Loi", "Dang nhap that bai",
                                    "Sai tai khoan hoac mat khau. Vui long thu lai."));
                    return;
                }

                if (!"SIGNIN_OK".equals(type) || !node.has("payloadJson")) {
                    return;
                }

                SigninResponsePayload payload =
                        gson.fromJson(node.get("payloadJson").asText(), SigninResponsePayload.class);

                Account account = buildAccount(payload);
                UserSession.setCurrentAccount(account);

                String viewFileName;
                String windowTitle;

                if (Account.ADMIN.equalsIgnoreCase(account.getRole())) {
                    viewFileName = "AdminInfo.fxml";
                    windowTitle = "Thong tin admin";
                } else {
                    viewFileName = "UserInfo.fxml";
                    windowTitle = "Thong tin nguoi dung";
                }

                FXMLLoader loader = ViewLoader.loader(viewFileName);
                Parent root = loader.load();
                Scene sceneMain = new Scene(root);

                Platform.runLater(() -> {
                    pendingStage.setScene(sceneMain);
                    pendingStage.setTitle(windowTitle);
                    pendingStage.centerOnScreen();
                    pendingStage.show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Loi", "Dang nhap that bai",
                                "Khong the xu ly phan hoi tu server."));
            }
        };

        MessageBus.getInstance().subscribe(signinResultHandler);
    }

    public void handle_signup(ActionEvent event) throws IOException {
        Parent signupRoot = ViewLoader.load("SignUp.fxml");
        Scene sceneSignup = new Scene(signupRoot);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(sceneSignup);
        window.setTitle("Dang ky tai khoan");
        window.centerOnScreen();
        window.show();
    }
    private Account buildAccount(SigninResponsePayload payload) {
        if (Account.ADMIN.equalsIgnoreCase(payload.getRole())) {
            Admin admin = new Admin(
                    payload.getId(),
                    payload.getName(),
                    payload.getEmail(),
                    payload.getPhoneNumber(),
                    payload.getPassword()
            );
            admin.setRole(payload.getRole());
            return admin;
        }

        User user = new User(
                payload.getId(),
                payload.getName(),
                payload.getEmail(),
                payload.getPhoneNumber(),
                payload.getPassword(),
                payload.getBalance()
        );
        user.setRole(payload.getRole());
        return user;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    private void cleanup() {
        if (signinResultHandler != null) {
            MessageBus.getInstance().unsubscribe(signinResultHandler);
        }
    }

}
