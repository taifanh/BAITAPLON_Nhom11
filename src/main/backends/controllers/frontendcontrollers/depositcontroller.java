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
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        subscribeDepositResult();

        Platform.runLater(() -> {
            Stage stage = (Stage) deposit_amount.getScene().getWindow();
            stage.setOnHidden(e -> cleanup());
        });
    }

    private void subscribeDepositResult() {
        depositHandler = rawJson -> {
//            Message msg = gson.fromJson(rawJson, Message.class);
//
//            if (!"deposit_result".equals(msg.messageType)) return;
//
//            if (msg.Id_user != UserSession.getCurrentUser().getId()) return;
//
//            DepositResult result = gson.fromJson(msg.payloadJson, DepositResult.class);
//
//            Platform.runLater(() -> {
//                if (result.isSuccess()) {
//                    money_display.setText(String.format("%,.0f", result.getNewBalance()));
//                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Nạp tiền thành công!");
//                    closeWindow();
//                } else {
//                    showAlert(Alert.AlertType.ERROR, "Thất bại", "Nạp tiền thất bại!");
//                }
//            });
        };

        MessageBus.getInstance().subscribe(depositHandler);
    }

    public void cleanup() {
        if (depositHandler != null) {
          MessageBus.getInstance().unsubscribe(depositHandler);
        }
    }

    public void ok_deposit(ActionEvent event) {
        try {
            double moneyIn = Double.parseDouble(deposit_amount.getText());

            Message msg = new Message();
            msg.Id_user = UserSession.getCurrentUser().getId();
            msg.messageType = "deposit";
            msg.payloadJson = gson.toJson(new Depositpayload(moneyIn));

            UserSession.getConnection().send(msg);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ");
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) deposit_amount.getScene().getWindow();
        stage.close();
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type); alert.setTitle(title);
        alert.setHeaderText(null); alert.setContentText(content);
        alert.showAndWait();
    }
}
