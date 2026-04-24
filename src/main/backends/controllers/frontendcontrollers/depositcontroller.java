package controllers.frontendcontrollers;

import com.google.gson.Gson;
import controllers.ServerConnection;
import controllers.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Extra.messages.Depositpayload;
import models.Extra.messages.Message;
import java.io.IOException;

public class depositcontroller {
    @FXML
    public TextField deposit_amount;

    @FXML
    public TextField name_deposit_input;
    @FXML
    public TextField phonenumber_deposit_input;
    @FXML
    public Label money_display;


    public void ok_deposit(ActionEvent event) throws IOException {
        if (UserSession.getCurrentUser() == null) {
            showAlert(Alert.AlertType.ERROR, "Loi", "Khong tim thay nguoi dung dang dang nhap.");
            return;
        }

        String amountText = deposit_amount.getText() == null ? "" : deposit_amount.getText().trim();
        if (amountText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Loi", "Vui long nhap so tien nap.");
            return;
        }

        double moneyIn;
        try {
            moneyIn = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Loi", "So tien khong hop le.");
            return;
        }
        if (moneyIn <= 0) {
            showAlert(Alert.AlertType.WARNING, "Loi", "So tien nap phai lon hon 0.");
            return;
        }

        Gson gson = new Gson();
        Depositpayload payload = new Depositpayload(moneyIn);
        String payloadJson = gson.toJson(payload);

        Message msg = new Message();
        msg.Id_user = UserSession.getCurrentUser().getId();
        msg.messageType = "deposit";
        msg.payloadJson = payloadJson;

        ServerConnection.send(msg);
        money_display.setText(amountText);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }

    public void handle_verify_money(ActionEvent event){
        String amountText = deposit_amount.getText() == null ? "" : deposit_amount.getText().trim();
        money_display.setText(amountText);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
