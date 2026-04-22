package controllers.frontendcontrollers;

import Database.UserStore;
import controllers.UserSession;
import controllers.ViewLoader;
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
import models.accounts.User;

import java.io.IOException;
import java.util.Optional;

public class signincontroller {
    @FXML
    public TextField txtphonenumberfield;

    @FXML
    public PasswordField txtpassfield;

    @FXML
    public Button signinbtn;

    @FXML
    public Button signupbtn;

    private final UserStore userStore = new UserStore();

    public void handle_signin(ActionEvent event) {
        String phoneNumber = txtphonenumberfield.getText() == null ? "" : txtphonenumberfield.getText().trim();
        String password = txtpassfield.getText() == null ? "" : txtpassfield.getText().trim();

        if (phoneNumber.isBlank() || password.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Loi", null, "Vui long nhap day du so dien thoai va mat khau.");
            return;
        }

        try {
            Optional<User> userOptional = userStore.authenticate(phoneNumber, password);
            if (userOptional.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Loi", "Dang nhap that bai", "Sai tai khoan hoac mat khau. Vui long thu lai.");
                return;
            }

            User user = userOptional.get();
            UserSession.setCurrentUser(user);

            FXMLLoader loader = ViewLoader.loader("userinfo.fxml");
            Parent root = loader.load();
            userinfocontroller controller = loader.getController();
            controller.setUser(user);

            Scene sceneMain = new Scene(root);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(sceneMain);
            window.setTitle("Thong tin nguoi dung");
            window.centerOnScreen();
            window.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Loi co so du lieu", "Khong the dang nhap", "Khong the doc du lieu nguoi dung tu SQLite.");
        }
    }

    public void handle_signup(ActionEvent event) throws IOException {
        Parent signupRoot = ViewLoader.load("signup.fxml");
        Scene sceneSignup = new Scene(signupRoot);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(sceneSignup);
        window.setTitle("Dang ky tai khoan");
        window.centerOnScreen();
        window.show();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
