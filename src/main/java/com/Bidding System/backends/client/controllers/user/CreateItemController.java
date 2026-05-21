package backends.client.controllers.user;

import backends.server.database.RequestLogDAO;
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
import backends.common.messages.Common.Createitempayload;
import backends.common.messages.Common.Message;

import java.io.IOException;
import java.util.function.Consumer;

public class CreateItemController {
    @FXML
    public TextArea item_info;

    @FXML
    public TextField base_price;

    @FXML
    public TextField bid_increment;

    @FXML
    public ComboBox<String> item_type;

    @FXML
    public TextField item_name;

    private Consumer<String> createitemHandler;

    private final RequestLogDAO requestLogDAO = new RequestLogDAO();

    public void handle_create_ok(ActionEvent event) throws IOException {
        String type = item_type.getSelectionModel().getSelectedItem().toString();
        double bprice = Double.parseDouble(base_price.getText());
        double bincrement = Double.parseDouble(bid_increment.getText());
        String iteminfo = item_info.getText();
        String itemname = item_name.getText();

        Gson gson = new Gson();
        Createitempayload createitempayload = new Createitempayload(type, itemname, iteminfo, bprice, bincrement);
        String payload = gson.toJson(createitempayload);

        Message msg = new Message();
        msg.payloadJson = payload;
        msg.messageType = "additem";
        msg.Id_user = UserSession.getCurrentUser().getId();

        UserSession.getConnection().send(msg);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }

    @FXML
    public void initialize() {
        ObservableList<String> categories = FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"
        );
        item_type.setItems(categories);

        subscribeCreateResult();
        if (createitemHandler != null) {
            MessageBus.getInstance().subscribe(createitemHandler);
        }

        Platform.runLater(() -> {
            Stage stage = (Stage) item_info.getScene().getWindow();
            stage.setOnHidden(e -> cleanup());
        });
    }

    public void handle_come_back(ActionEvent event) throws IOException {
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.close();
    }

    public void subscribeCreateResult() {
        createitemHandler = rawJson -> {
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
        if (createitemHandler != null) {
            MessageBus.getInstance().unsubscribe(createitemHandler);
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
        Stage stage = (Stage) item_type.getScene().getWindow();
        stage.close();
    }
}
