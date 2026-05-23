package com.bidding_system.backends.client.controllers.components;

import com.bidding_system.backends.client.session.UserSession;
import com.bidding_system.backends.common.messages.Common.Createitempayload;
import com.bidding_system.backends.common.messages.Common.Message;
import com.bidding_system.backends.common.messages.Common.RemoveRequestpayload;
import com.bidding_system.backends.server.database.MyRequestDAO;
import com.bidding_system.backends.server.database.RequestLogDAO;
import com.google.gson.Gson;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import java.io.IOException;

public class CustomItemCell extends ListCell<String> {
    private HBox content;
    private Label itemName;
    private Button viewInfo;
    private Button removeItem;
    private Pane spacer;
    private final RequestLogDAO requestLogDAO = new RequestLogDAO();
    private final MyRequestDAO myRequest = new MyRequestDAO();
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
            String requestId = getItem();
            if (requestId == null) {
                return;
            }
            try {
                MyRequestDAO.RequestRecord request = myRequest.findByRequestId(requestId);
                if (request == null) {
                    return;
                }
                Createitempayload payload = gson.fromJson(request.requestInfo(), Createitempayload.class);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thong tin item");
                alert.setHeaderText(payload.getItem_name());
                alert.setContentText(
                        "Request ID: " + request.requestId() + "\n" +
                                "User ID: " + request.userId() + "\n" +
                                "Type: " + payload.getItemType() + "\n" +
                                "Base price: " + payload.getBasePrice() + "\n" +
                                "Increment: " + payload.getBidIncrement() + "\n" +
                                "Info: " + payload.getItemInfo() + "\n" +
                                "Status: " + request.status() + "\n" +
                                "Time: " + request.time()
                );
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        removeItem.setOnAction(event ->{
            String item = getItem();// trả về id_request

            Gson gson = new Gson();
            RemoveRequestpayload payload = new RemoveRequestpayload(item , myRequest.getStatusById(item));
            String payloadJson = gson.toJson(payload);

            Message msg = new Message();
            msg.Id_user = UserSession.getCurrentUser().getId();
            msg.messageType = "removeitem";
            msg.payloadJson = payloadJson;

            UserSession.getConnection().send(msg);
            System.out.println("đã xóa item khỏi history");
        });


    }
    @Override
    protected void updateItem(String item , boolean empty){// javafx AUTO call it
        super.updateItem(item, empty);
        if(item!=null && !empty){
            try {
                RequestLogDAO.RequestRecord request = requestLogDAO.findByRequestId(item);
                if (request != null) {
                    Createitempayload payload = gson.fromJson(request.requestInfo(), Createitempayload.class);
                    itemName.setText(payload.getItem_name());
                } else {
                    itemName.setText(item);
                }
            } catch (IOException e) {
                itemName.setText(item);
            }
            setGraphic(content);
        }
        else
            setGraphic(null);
    }
}
