package backends.client.controllers.components;

import backends.common.messages.Common.CreateItemPayload;
import backends.common.messages.MsgData.RequestRecordDto;
import com.google.gson.Gson;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class CustomItemRequestCell extends ListCell<RequestRecordDto> {
    private final HBox content;
    private final Button view;
    private final Label name_item;
    private final CheckBox selected;
    private final Gson gson = new Gson();
    private final java.util.Set<String> selectedIds; // Tham chiếu đến RAM của Controller

    public CustomItemRequestCell(java.util.Set<String> selectedIds) {
        super();
        this.selectedIds = selectedIds;
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        name_item = new Label();
        selected = new CheckBox();
        view = new Button("view");

        content = new HBox(10, name_item, spacer, view, selected);
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
                            "Info: " + payload.getItemInfo()
            );
            alert.showAndWait();
        });

        selected.setOnAction(event -> {
            RequestRecordDto request = getItem();
            if (request != null) {
                if (selected.isSelected()) {
                    selectedIds.add(request.requestId); // Lưu vào RAM
                } else {
                    selectedIds.remove(request.requestId);
                }
            }
        });
    }

    @Override
    protected void updateItem(RequestRecordDto request, boolean empty) {
        super.updateItem(request, empty);
        if (request != null && !empty) {
            CreateItemPayload payload = gson.fromJson(request.requestInfo, CreateItemPayload.class); // ← thêm dòng này
            name_item.setText(payload.getItem_name());
            selected.setSelected(selectedIds.contains(request.requestId)); // ← thêm dòng này
            setGraphic(content);
        } else {
            setGraphic(null);
        }
    }
}
