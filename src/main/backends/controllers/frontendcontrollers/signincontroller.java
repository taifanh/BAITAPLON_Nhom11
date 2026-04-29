package controllers.frontendcontrollers;

import Database.UserStore;
import com.google.gson.Gson;
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
import models.Extra.messages.Message;
import models.Extra.messages.loginpayload;
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

    private final UserStore userStore = new UserStore();

    public void handle_signin(ActionEvent event) {
        String phoneNumber = txtphonenumberfield.getText() == null ? "" : txtphonenumberfield.getText().trim();
        String password = txtpassfield.getText() == null ? "" : txtpassfield.getText().trim();

        if (phoneNumber.isBlank() || password.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Loi", null, "Vui long nhap day du so dien thoai va mat khau.");
            return;
        }

        try {
            Optional<Account> accountOptional = userStore.authenticate(phoneNumber, password);
            if (accountOptional.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Loi", "Dang nhap that bai", "Sai tai khoan hoac mat khau. Vui long thu lai.");
                return;
            }

            Account account = accountOptional.get();
            UserSession.setCurrentAccount(account);
            String viewFileName;
            String windowTitle;


            //kiểm tra role để mở màn hình Info
            if ("Admin".equalsIgnoreCase(account.getRole())) {
                viewFileName = "admininfo.fxml";
                windowTitle = "Thong tin admin";
            } else {
                viewFileName = "userinfo.fxml";
                windowTitle = "Thong tin nguoi dung";
            }

            FXMLLoader loader = ViewLoader.loader(viewFileName);
            Parent root = loader.load();
            Object controller = loader.getController();
            if (account instanceof User user && controller instanceof userinfocontroller userController) {
                userController.setUser(user);
            }

            Scene sceneMain = new Scene(root);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(sceneMain);
            window.setTitle(windowTitle);
            window.centerOnScreen();
            window.show();

            // send message login to server to save identity for clienthandler
            loginpayload payload = new loginpayload(account.getRole());
            Gson gson = new  Gson();
            String payloadjson = gson.toJson(payload);

            Message msg = new Message();
            msg.Id_user = account.getId();
            msg.messageType = "login";
            msg.payloadJson = payloadjson;

            UserSession.getConnection().send(msg);

        } catch (IOException e) {
            e.printStackTrace();
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
