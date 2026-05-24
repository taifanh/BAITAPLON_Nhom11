package com.bidding_system.backends.client.controllers.user;

import backends.common.models.accounts.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import backends.common.messages.Common.Depositpayload;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.MessageType;
import java.util.function.Consumer;

public class DepositController {
    @FXML public TextField depositAmount;
    @FXML public TextField depositorNameInput;
    @FXML public TextField depositorPhoneNumberInput;
    @FXML public Label newBalance;
    @FXML public Button verifyButton;

    private User currentUser;
    private Consumer<String> depositHandler;
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        subscribeDepositResult();
        currentUser = UserSession.getCurrentUser();
        if (currentUser == null) return;
        Platform.runLater(() -> {
            Stage stage = (Stage) depositAmount.getScene().getWindow();
            stage.setOnHidden(e -> cleanup());
        });
    }

    @FXML
    public void handleVerify(ActionEvent event) {
        try {
            double moneyIn = Double.parseDouble(depositAmount.getText());
            double currentBalance = currentUser.getBalance(); //
            double expected = currentBalance + moneyIn;

            newBalance.setText(String.format("%.2f", expected));

        } catch (NumberFormatException e) {
            newBalance.setText("Số tiền không hợp lệ");
        }
    }

    private void subscribeDepositResult() {
        depositHandler = rawJson -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                ObjectNode node = (ObjectNode) mapper.readTree(rawJson);
                String type = node.get("type").asText();
                if (type.equals("deposit_OK") && node.has("payloadJson")) {
                    ObjectNode payloadNode = (ObjectNode) mapper.readTree(node.get("payloadJson").asText());
                    double latestBalance = payloadNode.get("amount").asDouble();
                    Platform.runLater(() -> {
                        showAlert(
                                Alert.AlertType.INFORMATION,
                                "thành công",
                                "Nạp tiền thành công!\n: " + String.format("%.2f", latestBalance)
                        );
                        closeWindow();
                    });
                }
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

    public void CompleteDeposit(ActionEvent event) {
        try {
            double moneyIn = Double.parseDouble(depositAmount.getText());
            if(moneyIn <= 0) {
                throw new Exception();
            }
            Message msg = new Message();
            msg.Id_user = currentUser.getId();
            msg.messageType = MessageType.DEPOSIT.getValue();
            msg.payloadJson = gson.toJson(new Depositpayload(moneyIn));

            UserSession.getConnection().send(msg);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ");
        }
    }


    private void closeWindow() {
        Stage stage = (Stage) depositAmount.getScene().getWindow();
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
