package backends.client.controllers.user;

import backends.client.network.MessageBus;
import backends.common.messages.Common.SigninPayload;
import backends.common.messages.Common.SigninResponsePayload;
import backends.common.models.accounts.Admin;
import backends.common.models.accounts.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import backends.client.session.UserSession;
import backends.client.controllers.ViewLoader;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Screen;
import javafx.stage.Stage;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.MessageType;
import backends.common.models.core.Account;

import java.io.IOException;
import java.util.function.Consumer;

public class SignInController {
    @FXML
    public TextField phoneNumberField;

    @FXML
    public PasswordField passwordField;

    @FXML
    public Button signInButton;

    @FXML
    public Button signUpButton;


    private final Gson gson = new Gson();
    private final ObjectMapper mapper = new ObjectMapper();
    private Consumer<String> signInResultHandler;
    private Stage pendingStage;

    @FXML
    public void initialize() {
        receiveSuccessfulSignIn();

        Platform.runLater(() -> {
            Stage stage = (Stage) phoneNumberField.getScene().getWindow();
            stage.setOnHidden(e -> cleanup());
        });
    }

    public void handleSignIn(ActionEvent event) {
        String phoneNumber = phoneNumberField.getText() == null ? "" : phoneNumberField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (phoneNumber.isBlank() || password.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Loi", null, "Vui long nhap day du so dien thoai va mat khau.");
            return;
        }
        // lấy stage hiên tại để thay đổi mà hình khi sign in thành công
        pendingStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Message msg = new Message();
        msg.messageType = MessageType.SIGNIN.getValue();
        msg.payloadJson = gson.toJson(new SigninPayload(phoneNumber, password));

        UserSession.getConnection().send(msg);// còn tín hiệu gửi login cũ thì sẽ cho client xử lý luôn nếu đăng nhập thành công
    }
    private void receiveSuccessfulSignIn() {
        signInResultHandler = rawJson -> {
            try {
                JsonNode node = mapper.readTree(rawJson);
                String type = node.path("type").asText("");

                if ("SIGNIN_FAIL".equals(type)) {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Loi",
                                    "Dang nhap that bai",
                                    "Sai tai khoan hoac mat khau."));
                    return;
                }

                if (!"SIGNIN_OK".equals(type) || !node.has("payloadJson")) return;

                SigninResponsePayload payload =
                        gson.fromJson(node.get("payloadJson").asText(),
                                SigninResponsePayload.class);
                Account account = buildAccount(payload);
                UserSession.setCurrentAccount(account);

                String fxml  = Account.ADMIN.equalsIgnoreCase(account.getRole())
                        ? "AdminProfile.fxml" : "HomePage.fxml";
                String title = Account.ADMIN.equalsIgnoreCase(account.getRole())
                        ? "Admin Dashboard" : "Home";

                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = ViewLoader.loader(fxml);
                        Parent root       = loader.load();
                        pendingStage.setScene(new Scene(root));
                        pendingStage.setTitle(title);
                        fitToVisibleScreen(pendingStage);
                        pendingStage.show();
                    } catch (IOException e) {
                        e.printStackTrace(); // xem stack trace trong console
                        showAlert(Alert.AlertType.ERROR, "Loi",
                                "Khong the tai giao dien",
                                "FXML error: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Loi",
                                "Loi xu ly phan hoi",
                                e.getMessage()));
            }
        };

        MessageBus.getInstance().subscribe(signInResultHandler);
    }

    public void handleSignUp(ActionEvent event) throws IOException {
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
        if (signInResultHandler != null) {
            MessageBus.getInstance().unsubscribe(signInResultHandler);
        }
    }

    private void fitToVisibleScreen(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setMaximized(false);
        stage.setMinWidth(1000);
        stage.setMinHeight(620);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

}
