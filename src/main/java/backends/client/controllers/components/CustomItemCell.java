package backends.client.controllers.components;

import backends.client.session.UserSession;
import backends.common.messages.Common.CreateItemPayload;
import backends.common.messages.Common.Message;
import backends.common.messages.Common.MessageType;
import backends.common.messages.Common.RemoveRequestPayload;
import backends.common.messages.MsgData.RequestRecordDto;
import com.google.gson.Gson;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class CustomItemCell extends ListCell<RequestRecordDto> {
    private HBox content;
    private Label itemName;
    private Button viewInfo;
    private Button removeItem;
    private Pane spacer;
    private final Gson gson = new Gson();

    public CustomItemCell(){
        super();
        itemName = new Label();
        viewInfo = new Button("view");
        removeItem = new Button("remove");

        // spacer sẽ là khoản trắng để tạo khoảng cách cho các tác vụ
        // đưa 2 nút button về bên phải -> tách ra khỏi label name

        spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        content = new HBox(10, itemName , spacer , viewInfo, removeItem);
        content.setAlignment(Pos.CENTER_LEFT);

        viewInfo.setOnAction(event ->{
            RequestRecordDto request = getItem();
            if (request == null) {
                return;
            }
                CreateItemPayload payload = gson.fromJson(request.requestInfo, CreateItemPayload.class);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thong tin item");
                alert.setHeaderText(payload.getItem_name());
                alert.setContentText(
                        "Request ID: " + request.requestId + "\n" +
                                "User ID: " + request.userId + "\n" +
                                "Type: " + payload.getItemType() + "\n" +
                                "Base price: " + payload.getBasePrice() + "\n" +
                                "Info: " + payload.getItemInfo() + "\n" +
                                "Status: " + request.status + "\n" +
                                "Time: " + request.time
                );
                alert.showAndWait();
        });
        removeItem.setOnAction(event ->{
            RequestRecordDto item = getItem();// trả về dạng request record lưu các thông tin cơ bản của request và item đó

            Gson gson = new Gson();
            RemoveRequestPayload payload = new RemoveRequestPayload(item.requestId , item.status);
            String payloadJson = gson.toJson(payload);

            Message msg = new Message();
            msg.Id_user = UserSession.getCurrentUser().getId();
            msg.messageType = MessageType.REMOVE_ITEM.getValue();
            msg.payloadJson = payloadJson;

            UserSession.getConnection().send(msg);
            System.out.println("đã xóa item khỏi history");
        });


    }
    @Override
    protected void updateItem(RequestRecordDto request , boolean empty){// javafx AUTO call it
        super.updateItem(request, empty);
        if (request != null && !empty) {
            CreateItemPayload payload = gson.fromJson(request.requestInfo, CreateItemPayload.class);
            itemName.setText(payload.getItem_name());
            setGraphic(content);
        } else {
            setGraphic(null);
        }
    }
}
