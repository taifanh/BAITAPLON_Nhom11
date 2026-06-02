package backends.client.controllers.user;

import backends.server.service.CreateItemService;
import backends.client.session.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;

public class CreateItemController {
    @FXML
    public TextArea itemInfo;

    @FXML
    public TextField basePrice;

    @FXML
    public ComboBox<String> itemType;

    @FXML
    public TextField itemName;

    @FXML
    public Label selectedImageLabel;

    private final CreateItemService createItemService = new CreateItemService();

    public void handleCreateItem(ActionEvent event) throws IOException {
        String type = itemType.getValue() == null ? "" : itemType.getValue().trim();
        String priceText = basePrice.getText() == null ? "" : basePrice.getText().trim();
        String itemInfo = this.itemInfo.getText() == null ? "" : this.itemInfo.getText().trim();
        String itemName = this.itemName.getText() == null ? "" : this.itemName.getText().trim();

        if (type.isEmpty() || priceText.isEmpty() || itemInfo.isEmpty() || itemName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Error", "Please enter all the information !");
            return;
        }

        double bidPrice;
        try {
            bidPrice = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Error", "Please enter a valid price !");
            return;
        }

        createItemService.submitCreateItem(
                UserSession.getCurrentUser().getId(),
                type,
                itemName,
                itemInfo,
                bidPrice
        );
    }

    @FXML
    public void initialize() {
        ObservableList<String> categories = FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"
        );
        itemType.setItems(categories);

        createItemService.setListener(new CreateItemService.Listener() {
            @Override
            public void onCreateSuccess() {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Successful", "add item successful!");
                    closeWindow();
                });
            }

            @Override
            public void onCreateFailure(String message) {
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.WARNING, "Failed", message));
            }
        });

        Platform.runLater(() -> {
            if (itemInfo.getScene() != null && itemInfo.getScene().getWindow() instanceof Stage stage) {
                stage.setOnHidden(e -> cleanup());
            }
        });
    }

    public void handleComeBack(ActionEvent event) throws IOException {
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }

    @FXML
    public void handleChooseImage(ActionEvent event) throws IOException {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose item image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        var selectedFile = chooser.showOpenDialog(stage);
        if (selectedFile == null) {
            return;
        }

        String selectedImageFileName = createItemService.selectImage(selectedFile.toPath());
        if (selectedImageLabel != null) {
            selectedImageLabel.setText(selectedImageFileName);
        }
    }

    public void cleanup() {
        createItemService.cleanup();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) itemType.getScene().getWindow();
        stage.close();
    }
}
