package backends.client.controllers.components;

import backends.common.messages.Common.CreateItemPayload;
import backends.common.messages.MsgData.RequestRecordDto;
import com.google.gson.Gson;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class CustomItemRequestCell extends ListCell<RequestRecordDto> {
    private final HBox content;
    private final Button view;
    private final Label nameItem;
    private final CheckBox selected;
    private final Gson gson = new Gson();
    private final java.util.Set<String> selectedIds;

    public CustomItemRequestCell(java.util.Set<String> selectedIds) {
        super();
        this.selectedIds = selectedIds;

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        nameItem = new Label();
        nameItem.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        nameItem.setWrapText(true);

        selected = new CheckBox();
        view = new Button("view");

        HBox nameRow = new HBox(8, nameItem);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        content = new HBox(10, nameRow, spacer, view, selected);
        content.setAlignment(Pos.CENTER_LEFT);

        view.setOnAction(event -> {
            RequestRecordDto request = getItem();
            if (request == null) return;

            CreateItemPayload payload = gson.fromJson(request.requestInfo, CreateItemPayload.class);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thong tin item");
            alert.setHeaderText(payload.getItem_name());
            alert.setContentText(
                    "Request ID: " + request.requestId + "\n" +
                            "User ID: " + request.userId + "\n" +
                            "Type: " + payload.getItemType() + "\n" +
                            "Base price: " + payload.getBasePrice() + "\n" +
                            "Info: " + payload.getItemInfo() + "\n"
            );
            alert.showAndWait();
        });

        selected.setOnAction(event -> {
            RequestRecordDto request = getItem();
            if (request == null) return;

            if (selected.isSelected()) {
                selectedIds.add(request.requestId);
            } else {
                selectedIds.remove(request.requestId);
            }
        });
    }

    @Override
    protected void updateItem(RequestRecordDto request, boolean empty) {
        super.updateItem(request, empty);
        if (request != null && !empty) {
            CreateItemPayload payload = gson.fromJson(request.requestInfo, CreateItemPayload.class);
            nameItem.setText(payload.getItem_name());
            selected.setSelected(selectedIds.contains(request.requestId));
            setGraphic(content);
        } else {
            setGraphic(null);
        }
    }
}
