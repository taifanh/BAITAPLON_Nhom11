package controllers;

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
import models.core.Account;

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

    private final UserJsonStore userJsonStore = new UserJsonStore();

    public void handle_signin(ActionEvent event) {
        String phoneNumber = txtphonenumberfield.getText() == null ? "" : txtphonenumberfield.getText().trim();
        String password = txtpassfield.getText() == null ? "" : txtpassfield.getText().trim();

        if (phoneNumber.isBlank() || password.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Loi", null, "Vui long nhap day du so dien thoai va mat khau.");
            return;
        }

        try {
            Optional<User> userOptional = userJsonStore.authenticate(phoneNumber, password);
            if (userOptional.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Loi", "Dang nhap that bai", "Sai tai khoan hoac mat khau. Vui long thu lai.");
                return;
            }

            UserSession.setCurrentUser(userOptional.get());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/views/userinfo.fxml"));
            Parent root = loader.load(); // phần giao diện người dùng
            userinfocontroller controller = loader.getController();
            controller.setUser(userOptional.get());

            Scene sceneMain = new Scene(root);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(sceneMain);
            window.setTitle("Thong tin nguoi dung");
            window.centerOnScreen();
            window.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Loi doc file", "Khong the dang nhap", "Khong the doc du lieu nguoi dung tu file JSON.");
        }
    }

    public void handle_signup(ActionEvent event) throws IOException {
        Parent signupRoot = FXMLLoader.load(getClass().getResource("/org/example/views/signup.fxml"));
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
