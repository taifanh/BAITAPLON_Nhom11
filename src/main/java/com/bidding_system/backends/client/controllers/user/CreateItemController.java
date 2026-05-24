package com.bidding_system.backends.client.controllers.user;

import com.bidding_system.backends.server.database.RequestLogDAO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.bidding_system.backends.client.network.MessageBus;
import com.bidding_system.backends.client.session.UserSession;
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
import com.bidding_system.backends.common.messages.Common.Createitempayload;
import com.bidding_system.backends.common.messages.Common.Message;

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

    private final RequestLogDAO requestLogDAO = new RequestLogDAO();

    public void handle_create_ok(ActionEvent event) throws IOException {
        // Validate trước khi xử lý
        if (itemType.getSelectionModel().getSelectedItem() == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn loại sản phẩm");
            return;
        }
        if (itemName.getText() == null || itemName.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập tên sản phẩm");
            return;
        }
        if (basePrice.getText() == null || basePrice.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập giá khởi điểm");
            return;
        }
        double bidPrice;
        try {
            bidPrice = Double.parseDouble(basePrice.getText());
            if (bidPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá khởi điểm phải là số dương");
            return;
        }
        if (itemInfo.getText() == null || itemInfo.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập mô tả sản phẩm");
            return;
        }

        String type     = itemType.getSelectionModel().getSelectedItem();
        String name     = itemName.getText().trim();
        String info     = itemInfo.getText().trim();

        Gson gson = new Gson();
        Createitempayload payload = new Createitempayload(type, name, info, bidPrice);

        Message msg = new Message();
        msg.payloadJson  = gson.toJson(payload);
        msg.messageType  = "additem";
        msg.Id_user      = UserSession.getCurrentUser().getId();

        UserSession.getConnection().send(msg);

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
                        showAlert(Alert.AlertType.INFORMATION, "Thanh cong", "tao san pham thanh cong!");
                        closeWindow();
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Khong thanh cong", "khong the tao san pham");
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
