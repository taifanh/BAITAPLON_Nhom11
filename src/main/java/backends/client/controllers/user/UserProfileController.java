package backends.client.controllers.user;

import backends.client.controllers.ViewLoader;
import backends.client.controllers.base.BaseController;
import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import backends.common.messages.Common.AvatarPayload;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.MessageType;
import backends.common.models.accounts.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.function.Consumer;

public class UserProfileController extends BaseController {

    private static final double AVATAR_FRAME_WIDTH = 240.0;
    private static final double AVATAR_FRAME_HEIGHT = 250.0;

    @FXML private Label labelName;
    @FXML private Label labelEmail;
    @FXML private Label labelPassword;
    @FXML private Label labelPhoneNumber;
    @FXML private Label labelBalance;
    @FXML private CheckBox checkShowPassword;
    @FXML private TextField changable_infoname;
    @FXML private ImageView avatarImageView;

    private static final String MSG_BALANCE_OK = "BALANCE_OK";
    private static final String MSG_DEPOSIT_OK = "deposit_OK";
    private static final String MSG_AVATAR_OK = "AVATAR_OK";
    private static final String MSG_AVATAR_SAVE_OK = "AVATAR_SAVE_OK";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private User currentUser;
    private Consumer<String> balanceHandler;
    private Consumer<String> avatarHandler;

    @FXML
    public void initialize() {
        currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        displayUserInfo(currentUser);
        subscribeAvatarUpdate();
        subscribeBalanceUpdate();
        requestLatestBalance();
        requestAvatar();

        checkShowPassword.selectedProperty()
                .addListener((obs, old, show) -> refreshPasswordLabel(show));
    }

    @Override
    public void cleanup() {
        if (balanceHandler != null) {
            MessageBus.getInstance().unsubscribe(balanceHandler);
        }
        if (avatarHandler != null) {
            MessageBus.getInstance().unsubscribe(avatarHandler);
        }
    }

    private void displayUserInfo(User user) {
        String name = user.getName() == null ? "" : user.getName();
        labelName.setText(name);
        if (changable_infoname != null) {
            changable_infoname.setText(name);
        }
        labelEmail.setText(user.getEmail());
        labelPhoneNumber.setText(user.getPhoneNumber());
        labelBalance.setText(String.valueOf(user.getBalance()));
        refreshPasswordLabel(checkShowPassword.isSelected());
    }

    private void refreshPasswordLabel(boolean visible) {
        if (currentUser == null) {
            return;
        }
        String pwd = currentUser.getPassword();
        labelPassword.setText(visible ? pwd : "*".repeat(pwd.length()));
    }

    private void requestLatestBalance() {
        var msg = new Message();
        msg.messageType = MessageType.GET_BALANCE.getValue();
        msg.Id_user = currentUser.getId();
        UserSession.getConnection().send(msg);
    }

    private void requestAvatar() {
        var msg = new Message();
        msg.messageType = MessageType.GET_AVATAR.getValue();
        msg.Id_user = currentUser.getId();
        UserSession.getConnection().send(msg);
    }

    private void subscribeBalanceUpdate() {
        balanceHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                String type = node.path("type").asText();

                if (MSG_BALANCE_OK.equals(type) && node.has("amount")) {
                    updateBalance(node.get("amount").asDouble());
                } else if (MSG_DEPOSIT_OK.equals(type) && node.has("payloadJson")) {
                    JsonNode payload = MAPPER.readTree(node.get("payloadJson").asText());
                    updateBalance(payload.get("amount").asDouble());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(balanceHandler);
    }

    private void subscribeAvatarUpdate() {
        avatarHandler = raw -> {
            try {
                JsonNode node = MAPPER.readTree(raw);
                String type = node.path("type").asText();

                if ((MSG_AVATAR_OK.equals(type) || MSG_AVATAR_SAVE_OK.equals(type))
                        && node.has("payloadJson")) {
                    JsonNode payload = MAPPER.readTree(node.get("payloadJson").asText());
                    // Server trả Base64 để client render trực tiếp, không cần path local.
                    String imageBase64 = payload.path("imageBase64").asText("");
                    String avatarPath = payload.path("avatarPath").asText("");
                    updateAvatarImage(imageBase64, avatarPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        MessageBus.getInstance().subscribe(avatarHandler);
    }

    private void updateBalance(double amount) {
        currentUser.setBalance(amount);
        Platform.runLater(() -> labelBalance.setText(String.valueOf(amount)));
    }

    private void updateAvatarImage(String imageBase64, String avatarPath) {
        Platform.runLater(() -> {
            if (avatarImageView == null) {
                return;
            }

            if (imageBase64 != null && !imageBase64.isBlank()) {
                // Preview ngay từ Base64 vừa gửi/nhận.
                try (InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(imageBase64))) {
                    Image image = new Image(in);
                    avatarImageView.setImage(image);
                    applyCenterCrop(avatarImageView, image, AVATAR_FRAME_WIDTH, AVATAR_FRAME_HEIGHT);
                    return;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (avatarPath == null || avatarPath.isBlank()) {
                avatarImageView.setImage(null);
                return;
            }

            // Fallback cũ: nếu chỉ có path server-side thì vẫn có thể đọc file local trong data/.
            Path imagePath = Path.of(avatarPath);
            if (Files.notExists(imagePath)) {
                avatarImageView.setImage(null);
                return;
            }

            try (InputStream in = Files.newInputStream(imagePath)) {
                Image image = new Image(in);
                avatarImageView.setImage(image);
                applyCenterCrop(avatarImageView, image, AVATAR_FRAME_WIDTH, AVATAR_FRAME_HEIGHT);
            } catch (IOException e) {
                e.printStackTrace();
                avatarImageView.setImage(null);
                avatarImageView.setViewport(null);
            }
        });
    }

    private void applyCenterCrop(ImageView imageView, Image image, double targetWidth, double targetHeight) {
        // Crop theo tâm để ảnh lấp đầy khung cố định, phần thừa bị cắt bỏ.
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) {
            imageView.setViewport(null);
            return;
        }

        double targetRatio = targetWidth / targetHeight;
        double imageRatio = imageWidth / imageHeight;
        Rectangle2D viewport;

        if (imageRatio > targetRatio) {
            double cropWidth = imageHeight * targetRatio;
            double x = (imageWidth - cropWidth) / 2.0;
            viewport = new Rectangle2D(x, 0, cropWidth, imageHeight);
        } else {
            double cropHeight = imageWidth / targetRatio;
            double y = (imageHeight - cropHeight) / 2.0;
            viewport = new Rectangle2D(0, y, imageWidth, cropHeight);
        }

        imageView.setFitWidth(targetWidth);
        imageView.setFitHeight(targetHeight);
        imageView.setPreserveRatio(true);
        imageView.setViewport(viewport);
    }

    @FXML
    public void handleUploadImage(ActionEvent event) throws IOException {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose avatar image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = getStage(event);
        var selectedFile = chooser.showOpenDialog(stage);
        if (selectedFile == null) {
            return;
        }

        // Client đọc file thành bytes rồi encode Base64 để gửi qua socket.
        byte[] avatarBytes = Files.readAllBytes(selectedFile.toPath());
        String imageBase64 = Base64.getEncoder().encodeToString(avatarBytes);
        String fileName = selectedFile.getName();

        var payload = new AvatarPayload(currentUser.getId(), null, fileName, imageBase64);
        var msg = new Message();
        msg.messageType = MessageType.SAVE_AVATAR.getValue();
        msg.Id_user = currentUser.getId();
        msg.payloadJson = MAPPER.writeValueAsString(payload);
        UserSession.getConnection().send(msg);

        // Preview tức thì bằng dữ liệu vừa chọn, không chờ round-trip server.
        updateAvatarImage(imageBase64, null);
    }

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
