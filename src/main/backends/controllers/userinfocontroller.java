package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.JSON_request.PlaceBid;
import models.accounts.User;

import java.io.IOException;
import java.io.*;
import java.net.Socket;

public class userinfocontroller {
    @FXML
    private TextField infoname;

    @FXML
    private Label infoemail;

    @FXML
    private Label infopassword;

    @FXML
    private Label infophonenumber;

    @FXML
    private CheckBox passshow;

    @FXML
    private TextField bidprice;

    @FXML
    private Button placebid;

    @FXML
    private TextField result_bid;

    private User user;

    @FXML
    public void initialize() {
        passshow.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
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
        Parent root = ViewLoader.load("signin.fxml");
        Scene scene = new Scene(root);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(scene);
        window.setTitle("Sign in");
        window.centerOnScreen();
        window.show();
    }

    @FXML
    public void handle_bidding(ActionEvent event) {
        String amount = bidprice.getText();
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Loi", "Chua co thong tin nguoi dung", "");
            return;
        }
        if (amount == null || amount.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Thieu du lieu", "Ban chua nhap gia bid", "");
            return;
        }

        new Thread(() -> {
            try(Socket socket = new Socket("localhost",9999);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(new PlaceBid(user.getId(), user.getName(), amount));
                out.println(json);
                String result = in.readLine();
                Platform.runLater(() -> result_bid.setText(result == null ? "Server khong tra ve du lieu" : result));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Loi ke noi", "Khong the ket noi toi Server", e.getMessage())
                );
            }
        }).start();
    }

    private void refreshPasswordField() {
        if (user == null) {
            return;
        }
        if (!passshow.isSelected()) {
            infopassword.setText("*".repeat(user.getPassword().length()));
        } else {
            infopassword.setText(user.getPassword());
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
