package backends.server.service;

import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import backends.common.messages.Common.CreateItemPayload;
import backends.common.messages.Common.ItemImagePayload;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class CreateItemService {
    public interface Listener {
        void onCreateSuccess();
        void onCreateFailure(String message);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Gson GSON = new Gson();

    private final PendingItemImageUpload imageUpload = new PendingItemImageUpload();
    private final Consumer<String> messageHandler = this::handleMessage;

    private Listener listener;
    private boolean awaitingCreateResponse;

    public CreateItemService() {
        MessageBus.getInstance().subscribe(messageHandler);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public String selectImage(Path filePath) throws IOException {
        byte[] imageBytes = Files.readAllBytes(filePath);
        String fileName = filePath.getFileName().toString();
        String imageBase64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
        imageUpload.setSelectedImage(fileName, imageBase64);
        return fileName;
    }

    public void submitCreateItem(String userId, String type, String itemName, String itemInfo, double bidPrice) {
        awaitingCreateResponse = true;
        CreateItemPayload payload = new CreateItemPayload(type, itemName, itemInfo, bidPrice);
        Message msg = new Message();
        msg.payloadJson = GSON.toJson(payload);
        msg.messageType = MessageType.ADD_ITEM.getValue();
        msg.Id_user = userId;
        UserSession.getConnection().send(msg);
    }

    public void cleanup() {
        MessageBus.getInstance().unsubscribe(messageHandler);
        imageUpload.clear();
    }

    private void handleMessage(String rawJson) {
        try {
            JsonNode node = MAPPER.readTree(rawJson);
            String type = node.path("type").asText("");

            if (!awaitingCreateResponse
                    && !"ITEM_IMAGE_SAVE_OK".equals(type)
                    && !"ITEM_IMAGE_FAIL".equals(type)) {
                return;
            }

            switch (type) {
                case "add_item_OK" -> handleAddItemOk(node.path("request_id").asText(""));
                case "ITEM_IMAGE_SAVE_OK" -> notifySuccessAndReset();
                case "ITEM_IMAGE_FAIL" -> notifyFailure(node.path("message").asText("Cannot save image"));
                case "add_item_FAIL" -> notifyFailure("cannot add your item!");
                default -> {
                    return;
                }
            }
        } catch (Exception e) {
            if (awaitingCreateResponse) {
                notifyFailure("cannot add your item!");
            }
        }
    }

    private void handleAddItemOk(String requestId) {
        awaitingCreateResponse = false;
        boolean shouldUploadImage = imageUpload.registerCreatedRequest(requestId);
        if (shouldUploadImage) {
            Message msg = imageUpload.buildUploadMessage(UserSession.getCurrentUser().getId());
            if (msg == null) {
                notifySuccessAndReset();
                return;
            }
            UserSession.getConnection().send(msg);
            return;
        }

        notifySuccessAndReset();
    }

    private void notifySuccessAndReset() {
        awaitingCreateResponse = false;
        imageUpload.clear();
        if (listener != null) {
            listener.onCreateSuccess();
        }
    }

    private void notifyFailure(String message) {
        awaitingCreateResponse = false;
        imageUpload.clear();
        if (listener != null) {
            listener.onCreateFailure(message);
        }
    }

    private static final class PendingItemImageUpload {
        private final ObjectMapper mapper = new ObjectMapper();
        private String selectedImageBase64;
        private String selectedImageFileName;
        private String pendingRequestId;

        void setSelectedImage(String fileName, String imageBase64) {
            this.selectedImageFileName = fileName;
            this.selectedImageBase64 = imageBase64;
        }

        boolean hasSelectedImage() {
            return selectedImageBase64 != null && !selectedImageBase64.isBlank();
        }

        boolean registerCreatedRequest(String requestId) {
            this.pendingRequestId = requestId;
            return hasSelectedImage() && pendingRequestId != null && !pendingRequestId.isBlank();
        }

        Message buildUploadMessage(String userId) {
            if (!hasSelectedImage() || pendingRequestId == null || pendingRequestId.isBlank()) {
                return null;
            }

            ItemImagePayload payload = new ItemImagePayload(
                    pendingRequestId,
                    null,
                    null,
                    selectedImageFileName,
                    selectedImageBase64
            );

            Message msg = new Message();
            msg.messageType = MessageType.SAVE_ITEM_IMAGE.getValue();
            msg.Id_user = userId;
            try {
                msg.payloadJson = mapper.writeValueAsString(payload);
            } catch (Exception e) {
                throw new RuntimeException("Khong the tao payload anh san pham.", e);
            }
            return msg;
        }

        void clear() {
            selectedImageBase64 = null;
            selectedImageFileName = null;
            pendingRequestId = null;
        }
    }
}
