package controllers.frontendcontrollers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import controllers.MessageBus;
import controllers.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Extra.messages.Depositpayload;
import models.Extra.messages.Message;
import java.util.function.Consumer;

public class depositcontroller {
    @FXML public TextField deposit_amount;
    @FXML public TextField name_deposit_input;
    @FXML public TextField phonenumber_deposit_input;
    @FXML public Label money_display;
    @FXML public Button verify_btn;

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

    @FXML
    public void handle_verify(ActionEvent event) {
        try {
            double moneyIn = Double.parseDouble(deposit_amount.getText());
            double currentBalance = UserSession.getCurrentUser().getBalance();
            double expected = currentBalance + moneyIn;

            // Hiển thị số dư dự kiến vào label Tổng
            money_display.setText(String.format("%.2f", expected));

        } catch (NumberFormatException e) {
            money_display.setText("Số tiền không hợp lệ");
        }
    }

    private void subscribeDepositResult() {
        depositHandler = rawJson -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                ObjectNode node = (ObjectNode) mapper.readTree(rawJson);
                String type = node.get("type").asText();
                Platform.runLater(() -> {
                    if (type.equals("deposit_OK")) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Nạp tiền thành công!");
                        closeWindow();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Thất bại", "Nạp tiền thất bại!");
                    }
                });
        } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
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
            msg.messageType = "DEPOSIT";
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
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
