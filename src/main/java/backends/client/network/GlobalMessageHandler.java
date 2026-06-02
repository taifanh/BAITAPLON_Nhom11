package backends.client.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public final class GlobalMessageHandler {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private GlobalMessageHandler() {}
    public static void register() {
        MessageBus.getInstance().subscribe(rawJson -> {
            try {
                JsonNode node = MAPPER.readTree(rawJson);
                String type = node.path("type").asText("");
                if ("SERVER_SHUTDOWN".equals(type)) {
                    System.out.println("SHUTDOWN");
                    Platform.runLater(() -> {
                        Alert alert =
                                new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thông báo");
                        alert.setHeaderText(null);
                        alert.setContentText(
                                node.path("message")
                                        .asText("Server đã tắt.")
                        );
                        alert.showAndWait();
                        Platform.exit();
                    });
                }
            } catch (Exception ignored) {
            }
        });
    }
}