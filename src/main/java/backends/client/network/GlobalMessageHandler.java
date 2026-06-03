package backends.client.network;

import backends.client.session.UserSession;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public final class GlobalMessageHandler {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private GlobalMessageHandler() {}

    public static void register() {
        MessageBus.getInstance().subscribe(rawJson -> {
            try {
                JsonNode node = MAPPER.readTree(rawJson);
                String type = node.path("type").asText("");

                switch (type) {
                    case "SERVER_SHUTDOWN" -> handleServerShutdown(node);
                    case "AUCTION_RESULT"  -> handleAuctionResult(node);
                }

            } catch (Exception ignored) {}
        });
    }

    // ── Handlers ──────────────────────────────────────────────────

    private static void handleServerShutdown(JsonNode node) {
        System.out.println("SHUTDOWN");
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText(node.path("message").asText("Server đã tắt."));
            alert.showAndWait();
            Platform.exit();
        });
    }

    private static void handleAuctionResult(JsonNode node) {
        if (UserSession.getCurrentUser() == null) return;

        String userId    = UserSession.getCurrentUser().getId();
        String sellerId  = node.path("sellerId").asText();
        String winnerId  = node.path("winnerId").asText();
        String itemName  = node.path("itemName").asText();
        double amount    = node.path("winningAmount").asDouble();
        boolean hasBidder = node.path("hasBidder").asBoolean();

        if (hasBidder && userId.equals(sellerId)) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Auction result !");
                alert.setHeaderText("Item Sold");
                alert.setContentText(
                        "Item: "        + itemName   + "\n" +
                                "Sold for: "    + amount
                );
                alert.show();
            });
            requestLatestBalance(userId);
        }

        if (hasBidder && userId.equals(winnerId)) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Auction result !");
                alert.setHeaderText("Congratulation ! You Won !");
                alert.setContentText(
                        "Item: "   + itemName + "\n" +
                                "Amount: " + amount + "."
                );
                alert.show();
            });
        }
    }

    private static void requestLatestBalance(String userId) {
        if (userId == null || userId.isBlank() || UserSession.getConnection() == null) {
            return;
        }

        Message msg = new Message();
        msg.messageType = MessageType.GET_BALANCE.getValue();
        msg.Id_user = userId;
        UserSession.getConnection().send(msg);
    }
}
