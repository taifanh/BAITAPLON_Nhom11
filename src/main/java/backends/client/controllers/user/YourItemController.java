package backends.client.controllers.user;

import backends.client.controllers.base.BaseController;
import backends.client.controllers.components.BuyerItemCell;
import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import backends.common.messages.MsgData.BuyerItemResponse;
import backends.common.messages.MsgData.FetchBuyerToAuctionRequest;
import backends.common.messages.MsgData.ItemRecordDto;
import backends.common.models.accounts.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.util.function.Consumer;

public class YourItemController  extends BaseController  {
    @FXML private ListView<ItemRecordDto> itemListView;
    @FXML private Label itemCountLabel;

    private static final String MSG_BUY_ITEM_OK = "BUY_ITEM_RESPONSE";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ObservableList<ItemRecordDto> bought_item =
            FXCollections.observableArrayList();

    private Consumer<String> getItemHandler;

    @FXML
    private void initialize() throws IOException {
        System.out.println("YourItemController initialize");
        sendGetItemRequest();
        handleGetItemOk();
    }

    @FXML
    public void cleanup(){
        if(getItemHandler !=null) MessageBus.getInstance().unsubscribe(getItemHandler);
    }

    public void sendGetItemRequest() throws IOException {
        User currentUser = UserSession.getCurrentUser();
        if(currentUser ==null){
            System.out.println("Current user is null");
            return;
        }

        System.out.println("Sending request for user: " + currentUser.getId());
        FetchBuyerToAuctionRequest request = new FetchBuyerToAuctionRequest(currentUser.getId(), "");
        System.out.println("Request type: " + request.type);
        UserSession.getConnection().send(request);
    }

    @FXML
    public void handleGetItemOk() {
        getItemHandler = rawJson -> {
            System.out.println("Received response: " + rawJson);
            try {
                JsonNode node =  MAPPER.readTree(rawJson);
                if (!MSG_BUY_ITEM_OK.equals(node.get("type").asText())) {
                    System.out.println("Response type mismatch: " + node.get("type").asText());
                    return;
                }

                BuyerItemResponse response = MAPPER.readValue(rawJson, BuyerItemResponse.class);
                System.out.println("Items received: " + (response.itemlist != null ? response.itemlist.size() : 0));

                Platform.runLater(() -> {
                    bought_item.clear();

                    if (response.itemlist != null) {
                        bought_item.addAll(response.itemlist);
                        System.out.println("Added items to list: " + bought_item.size());
                    }

                    refreshListView();
                });

            } catch (Exception e){
                System.out.println("Error handling item response: " + e.getMessage());
                e.printStackTrace();
            }
        };
        System.out.println("Subscribed to MessageBus");
        MessageBus.getInstance().subscribe(getItemHandler);
    }

    private void refreshListView() {
        System.out.println("Refreshing ListView with " + bought_item.size() + " items");
        itemListView.setItems(bought_item);
        itemListView.setCellFactory(lv -> new BuyerItemCell());
        updateItemCount();
    }

    private void updateItemCount() {
        int count = bought_item.size();
        System.out.println("Updating item count: " + count);
        itemCountLabel.setText(String.format("Total: %d items", count));
    }
}
