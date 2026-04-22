package controllers.frontendcontrollers;

import Database.UserStore;
import controllers.ViewLoader;
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
import models.accounts.User;

import java.io.IOException;

public class signupcontroller {
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

    private final UserStore userStore = new UserStore();

    public void handle_signin(ActionEvent event) throws IOException {
        Parent signinRoot = ViewLoader.load("signin.fxml");
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

        try {
            if (userStore.phoneNumberExists(phoneNumber)) {
                showAlert(Alert.AlertType.WARNING, "Trung du lieu", null, "So dien thoai da ton tai.");
                return;
            }

            userStore.saveUser(new User(name, phoneNumber, email, password));
            showAlert(Alert.AlertType.INFORMATION, "OK", null, "Dang ky thanh cong. Du lieu da duoc luu vao SQLite.");

            Parent signinRoot = ViewLoader.load("signin.fxml");
            Scene sceneSignin = new Scene(signinRoot);

            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(sceneSignin);
            window.setTitle("Sign in");
            window.centerOnScreen();
            window.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Loi co so du lieu", "Khong the tao tai khoan", "Khong the ghi du lieu nguoi dung vao SQLite.");
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
