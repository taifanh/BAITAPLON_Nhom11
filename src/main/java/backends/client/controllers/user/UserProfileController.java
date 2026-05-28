package backends.client.controllers.user;

import backends.client.controllers.ViewLoader;
import backends.client.controllers.base.BaseController;
import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import backends.common.messages.Common.Message;
import backends.common.models.accounts.User;
import backends.common.messages.Common.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class UserProfileController extends BaseController {

    // ── FXML fields ──────────────────────────────────────────────
    @FXML private Label labelName;
    @FXML private Label labelEmail;
    @FXML private Label labelPassword;
    @FXML private Label labelPhoneNumber;
    @FXML private Label labelBalance;
    @FXML private CheckBox checkShowPassword;

    // ── Constants ─────────────────────────────────────────────────
    private static final String MSG_BALANCE_OK  = "BALANCE_OK";
    private static final String MSG_DEPOSIT_OK  = "deposit_OK";
    private static final String MSG_GET_BALANCE = "GET_BALANCE";

    // ── State ─────────────────────────────────────────────────────
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private User currentUser;
    private Consumer<String> balanceHandler;

    // ── Lifecycle ─────────────────────────────────────────────────
    @FXML
    public void initialize() {
        currentUser = UserSession.getCurrentUser();
        if (currentUser == null) return;

        displayUserInfo(currentUser);
        requestLatestBalance();
        subscribeBalanceUpdate();

        checkShowPassword.selectedProperty()
                .addListener((obs, old, show) -> refreshPasswordLabel(show));
    }

    @Override
    public void cleanup() {
        if (balanceHandler != null)
            MessageBus.getInstance().unsubscribe(balanceHandler);
    }

    // ── UI helpers ────────────────────────────────────────────────
    private void displayUserInfo(User user) {
        labelName.setText(user.getName());
        labelEmail.setText(user.getEmail());
        labelPhoneNumber.setText(user.getPhoneNumber());
        labelBalance.setText(String.valueOf(user.getBalance()));
        refreshPasswordLabel(checkShowPassword.isSelected());
    }

    private void refreshPasswordLabel(boolean visible) {
        if (currentUser == null) return;
        String pwd = currentUser.getPassword();
        labelPassword.setText(visible ? pwd : "*".repeat(pwd.length()));
    }

    private void requestLatestBalance() {
        var msg = new Message();
        msg.messageType = MessageType.GET_BALANCE.getValue();
        msg.Id_user     = currentUser.getId();
        UserSession.getConnection().send(msg);
    }

    // ── MessageBus subscription ───────────────────────────────────
    private void subscribeBalanceUpdate() {
        balanceHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                String type  = node.path("type").asText();

                if (MSG_BALANCE_OK.equals(type) && node.has("amount")) {
                    updateBalance(node.get("amount").asDouble());
                } else if (MSG_DEPOSIT_OK.equals(type) && node.has("payloadJson")) {
                    JsonNode payload = MAPPER.readTree(
                            node.get("payloadJson").asText());
                    updateBalance(payload.get("amount").asDouble());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(balanceHandler);
    }

    private void updateBalance(double amount) {
        currentUser.setBalance(amount);
        Platform.runLater(() -> labelBalance.setText(String.valueOf(amount)));
    }

    // ── Button handlers ───────────────────────────────────────────
    @FXML
    public void handleDeposit(ActionEvent event) throws IOException {
        FXMLLoader loader = ViewLoader.loader("Deposite.fxml");
        Parent root = loader.load();
        Stage popup = new Stage();
        double width = root instanceof Region region && region.getPrefWidth() > 0
                ? region.getPrefWidth()
                : 372;
        double height = root instanceof Region region && region.getPrefHeight() > 0
                ? region.getPrefHeight()
                : 514;
        popup.setResizable(false);
        popup.setScene(new Scene(root, width, height));
        popup.setTitle("Deposit");
        popup.setWidth(width);
        popup.setHeight(height);
        popup.centerOnScreen();
        popup.show();
    }
}
