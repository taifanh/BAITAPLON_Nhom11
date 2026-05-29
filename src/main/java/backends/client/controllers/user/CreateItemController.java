package backends.client.controllers.user;

import backends.common.messages.Common.CreateItemPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.MessageType;

import java.io.IOException;
import java.util.function.Consumer;

public class CreateItemController {
    @FXML
    public TextArea itemInfo;

    @FXML
    public TextField basePrice;

    @FXML
    public ComboBox<String> itemType;

    @FXML
    public TextField itemName;

    private Consumer<String> createItemHandler;

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

        Gson gson = new Gson();
        CreateItemPayload createitempayload = new CreateItemPayload(type, itemName, itemInfo, bidPrice);
        String payload = gson.toJson(createitempayload);
        Message msg = new Message();
        msg.payloadJson = payload;
        msg.messageType = MessageType.ADD_ITEM.getValue();
        msg.Id_user = UserSession.getCurrentUser().getId();

        UserSession.getConnection().send(msg);
        System.out.println("ADD_ITEM");
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }

    @FXML
    public void initialize() {
        ObservableList<String> categories = FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"
        );
        itemType.setItems(categories);

        subscribeCreateResult();
        if (createItemHandler != null) {
            MessageBus.getInstance().subscribe(createItemHandler);
        }

        Platform.runLater(() -> {
            Stage stage = (Stage) itemInfo.getScene().getWindow();
            stage.setOnHidden(e -> cleanup());
        });
    }

    public void handleComeBack(ActionEvent event) throws IOException {
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }

    public void subscribeCreateResult() {
        createItemHandler = rawJson -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                ObjectNode node = (ObjectNode) mapper.readTree(rawJson);
                String type = node.get("type").asText();

                Platform.runLater(() -> {
                    if (type.equals("add_item_OK")) {
                        showAlert(Alert.AlertType.INFORMATION, "Successful", "add item successful!");
                        closeWindow();
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Failed", "cannot add your item!");
                    }
                });
            } catch (JsonMappingException e) {
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public void cleanup() {
        if (createItemHandler != null) {
            MessageBus.getInstance().unsubscribe(createItemHandler);
        }
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
