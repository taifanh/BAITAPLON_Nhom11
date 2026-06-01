package backends.client.controllers.user;

import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import backends.common.messages.Common.ItemImagePayload;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.MessageType;
import backends.common.models.core.Item;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.function.Consumer;

public class ItemImageViewController {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MSG_ITEM_IMAGE_OK = "ITEM_IMAGE_OK";

    @FXML private ImageView itemImageView;
    @FXML private Label labelItemName;
    @FXML private Label labelItemType;
    @FXML private Label labelItemPrice;
    @FXML private Label labelItemInfo;
    @FXML private Label labelItemId;
    @FXML private Label labelImageStatus;

    private Item currentItem;
    private boolean initialized;
    private Consumer<String> imageHandler;

    @FXML
    public void initialize() {
        subscribeImageUpdates();
        initialized = true;
        if (currentItem != null) {
            renderItemInfo();
            requestImage();
        }
    }

    public void setItem(Item item) {
        this.currentItem = item;
        if (initialized) {
            renderItemInfo();
            requestImage();
        }
    }

    public void cleanup() {
        if (imageHandler != null) {
            MessageBus.getInstance().unsubscribe(imageHandler);
        }
    }

    @FXML
    public void handleClose(ActionEvent event) {
        ((Node) event.getSource()).getScene().getWindow().hide();
    }

    private void renderItemInfo() {
        if (currentItem == null) {
            return;
        }

        Platform.runLater(() -> {
            labelItemName.setText(currentItem.getName() != null ? currentItem.getName() : "");
            labelItemType.setText("Type: " + currentItem.getType());
            labelItemPrice.setText(String.format("Opening price: %,.0f VND", currentItem.getPrices()));
            labelItemInfo.setText(currentItem.getInfo() != null ? currentItem.getInfo() : "");
            labelItemId.setText("Item ID: " + currentItem.getId());
            labelImageStatus.setText("Loading image...");
        });
    }

    private void requestImage() {
        if (currentItem == null || currentItem.getId() == null || currentItem.getId().isBlank()) {
            return;
        }

        ItemImagePayload payload = new ItemImagePayload(null, currentItem.getId(), null, null, null);
        Message msg = new Message();
        msg.messageType = MessageType.GET_ITEM_IMAGE.getValue();
        msg.Id_user = UserSession.getCurrentUser() != null ? UserSession.getCurrentUser().getId() : null;
        try {
            msg.payloadJson = MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        UserSession.getConnection().send(msg);
    }

    private void subscribeImageUpdates() {
        imageHandler = rawJson -> {
            try {
                JsonNode node = MAPPER.readTree(rawJson);
                if (!MSG_ITEM_IMAGE_OK.equals(node.path("type").asText(""))) {
                    return;
                }

                ItemImagePayload payload = MAPPER.readValue(node.path("payloadJson").asText("{}"), ItemImagePayload.class);
                if (currentItem == null || payload.itemId == null || !payload.itemId.equals(currentItem.getId())) {
                    return;
                }

                Platform.runLater(() -> applyImage(payload.imageBase64, payload.imagePath));
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(imageHandler);
    }

    private void applyImage(String imageBase64, String imagePath) {
        if (imageBase64 != null && !imageBase64.isBlank()) {
            try (ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(imageBase64))) {
                itemImageView.setImage(new Image(in));
                labelImageStatus.setText("Image loaded");
                return;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        itemImageView.setImage(null);
        if (imagePath == null || imagePath.isBlank()) {
            labelImageStatus.setText("No image available");
        } else {
            labelImageStatus.setText("Image file not found");
        }
    }
}
