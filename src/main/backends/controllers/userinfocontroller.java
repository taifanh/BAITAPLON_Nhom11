package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.User;

import java.io.IOException;

public class userinfocontroller {
    @FXML
    private TextField infoname;

    @FXML
    private TextField infoemail;

    @FXML
    private TextField infopassword;

    @FXML
    private TextField infophonenumber;

    @FXML
    private CheckBox passhide;

    private User user;

    @FXML
    public void initialize() {
        passhide.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
        if (UserSession.getCurrentUser() != null) {
            setUser(UserSession.getCurrentUser());
        }
    }

    public void setUser(User user) {
        this.user = user;
        if (user == null) {
            return;
        }

        infoname.setText(user.getName());
        infoemail.setText(user.getEmail());
        infophonenumber.setText(user.getPhoneNumber());
        refreshPasswordField();
    }

    @FXML
    public void handle_sign_out(ActionEvent event) throws IOException {
        UserSession.clear();
        Parent root = FXMLLoader.load(getClass().getResource("signin.fxml"));
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
        if (passhide.isSelected()) {
            infopassword.setText("*".repeat(user.getPassword().length()));
        } else {
            infopassword.setText(user.getPassword());
        }
    }
}
