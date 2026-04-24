package controllers.frontendcontrollers;

import com.google.gson.Gson;
import controllers.MessageBus;
import controllers.ServerConnection;
import controllers.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Extra.messages.Depositpayload;
import models.Extra.messages.Message;
import java.io.IOException;
import java.util.function.Consumer;

public class depositcontroller {
    @FXML
    public TextField deposit_amount;

    @FXML
    public TextField name_deposit_input;
    @FXML
    public TextField phonenumber_deposit_input;
    @FXML
    public Label money_display;

    private Consumer<String> depositHandler;
    @FXML
    public void initialize() {
        subscribeDepositResult();
        Platform.runLater(() -> {
            Stage stage = (Stage) deposit_amount.getScene().getWindow();
            stage.setOnHidden(e -> cleanup());
        });
    }

    private void subscribeDepositResult() {
//        depositHandler = rawJson -> {
//            Message msg = gson.fromJson(rawJson, Message.class);
//
//            if (!"deposit_result".equals(msg.messageType)) return;
//
//            // parse payload
//            DepositResult result = gson.fromJson(msg.payloadJson, DepositResult.class);
//
//            Platform.runLater(() -> {
//                if (result.isSuccess()) {
//                    money_display.setText(String.format("%,.0f", result.getNewBalance()));
//                    showAlert("Thành công", "Nạp tiền thành công!");
//
//                    // đóng cửa sổ nếu muốn
//                    // closeWindow();
//
//                } else {
//                    showAlert("Thất bại", "Nạp tiền thất bại!");
//                }
//            });
//        };

        MessageBus.getInstance().subscribe(depositHandler);
    }

    public void cleanup() {
        if (depositHandler != null) {
            MessageBus.getInstance().unsubscribe(depositHandler);
        }
    }


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

        UserSession.getConnection().send(msg);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }


    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
