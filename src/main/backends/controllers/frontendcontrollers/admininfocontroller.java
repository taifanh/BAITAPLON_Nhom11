package controllers.frontendcontrollers;

import controllers.AdminService;
import controllers.UserSession;
import controllers.ViewLoader;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import models.accounts.User;
import models.core.Item;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class admininfocontroller {
    @FXML
    private TextField infoname;

    @FXML
    private Label infoemail;

    @FXML
    private Label infopassword;

    @FXML
    private Label infophonenumber;

    @FXML
    private CheckBox passshow;

    @FXML
    private ListView<AdminService.PendingItemRequest> ITEMLIST11;

    @FXML
    private ListView<InventoryEntry> ITEMLIST111;

    @FXML
    private ListView<InventoryEntry> ITEMLIST1;

    @FXML
    private Label itemname1;

    @FXML
    private TextField baseprice1;

    @FXML
    private TextField increment1;

    private User user;

    private final ObservableList<AdminService.PendingItemRequest> requestItems = FXCollections.observableArrayList();
    private final ObservableList<InventoryEntry> inventoryItems = FXCollections.observableArrayList();
    private final ObservableList<InventoryEntry> upcomingItems = FXCollections.observableArrayList();
    private final Map<Integer, BooleanProperty> requestSelection = new HashMap<>();

    @FXML
    public void initialize() {
        if (passshow != null) {
            passshow.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPasswordField());
        }

        setupLists();

        User currentUser = UserSession.getCurrentUser();
        if (currentUser != null) {
            setUser(currentUser);
        }

        refreshAdminData();
    }

    public void setUser(User user) {
        this.user = user;
        if (user == null) {
            return;
        }

        if (infoname != null) {
            infoname.setText(user.getName());
        }
        if (infoemail != null) {
            infoemail.setText(user.getEmail());
        }
        if (infophonenumber != null) {
            infophonenumber.setText(user.getPhoneNumber());
        }
        refreshPasswordField();
    }

    private void setupLists() {
        if (ITEMLIST11 != null) {
            ITEMLIST11.setItems(requestItems);
            ITEMLIST11.setCellFactory(CheckBoxListCell.forListView(
                    item -> requestSelection.computeIfAbsent(item.requestId(), key -> new SimpleBooleanProperty(false)),
                    new StringConverter<>() {
                        @Override
                        public String toString(AdminService.PendingItemRequest object) {
                            return object == null ? "" : object.toString();
                        }

                        @Override
                        public AdminService.PendingItemRequest fromString(String string) {
                            return null;
                        }
                    }
            ));
        }

        if (ITEMLIST111 != null) {
            ITEMLIST111.setItems(inventoryItems);
            ITEMLIST111.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                    showItemSummary(newValue));
        }

        if (ITEMLIST1 != null) {
            ITEMLIST1.setItems(upcomingItems);
            ITEMLIST1.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    showItemSummary(newValue);
                }
            });
        }
    }

    private void refreshPasswordField() {
        if (user == null || infopassword == null || passshow == null) {
            return;
        }

        infopassword.setText(passshow.isSelected()
                ? user.getPassword()
                : "*".repeat(user.getPassword().length()));
    }

    @FXML
    public void handle_accept_requests(ActionEvent event) {
        List<Integer> selectedRequestIds = getSelectedRequestIds();
        if (selectedRequestIds.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Requests", "Chua chon request", "Hay tick it nhat mot request de accept.");
            return;
        }

        try {
            AdminService.acceptRequests(selectedRequestIds);
            refreshAdminData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Requests", "Khong the accept request", e.getMessage());
        }
    }

    @FXML
    public void handle_reject_requests(ActionEvent event) {
        List<Integer> selectedRequestIds = getSelectedRequestIds();
        if (selectedRequestIds.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Requests", "Chua chon request", "Hay tick it nhat mot request de reject.");
            return;
        }

        try {
            AdminService.rejectRequests(selectedRequestIds);
            refreshAdminData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Requests", "Khong the reject request", e.getMessage());
        }
    }

    @FXML
    public void handle_start_auction(ActionEvent event) {
        InventoryEntry selectedInventoryItem = ITEMLIST111 == null
                ? null
                : ITEMLIST111.getSelectionModel().getSelectedItem();
        if (selectedInventoryItem == null) {
            showAlert(Alert.AlertType.INFORMATION, "Inventory", "Chua chon item", "Hay chon mot item trong inventory de dua vao bidding.");
            return;
        }

        try {
            AdminService.moveInventoryItemToBidding(selectedInventoryItem.itemId());
            refreshAdminData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Inventory", "Khong the bat dau bidding", e.getMessage());
        }
    }

    @FXML
    public void handle_sign_out(ActionEvent event) throws IOException {
        UserSession.clear();
        Parent root = ViewLoader.load("signin.fxml");
        Scene scene = new Scene(root);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(scene);
        window.setTitle("Sign in");
        window.centerOnScreen();
        window.show();
    }

    public void autobid(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Bidding", "Chua ho tro", "Phan dieu khien auction chi moi duoc noi du lieu danh sach.");
    }

    public void placebid(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Bidding", "Chua ho tro", "Huy phien dau gia se can noi tiep voi AuctionService.");
    }

    private void refreshAdminData() {
        try {
            List<AdminService.PendingItemRequest> pendingRequests = AdminService.getPendingItemRequests();
            requestItems.setAll(pendingRequests);
            requestSelection.keySet().retainAll(pendingRequests.stream().map(AdminService.PendingItemRequest::requestId).toList());
            pendingRequests.forEach(request -> requestSelection.computeIfAbsent(request.requestId(), key -> new SimpleBooleanProperty(false)));

            inventoryItems.setAll(AdminService.getWaitingInventoryItems().stream().map(InventoryEntry::fromItem).toList());
            upcomingItems.setAll(AdminService.getItemsInAuction().stream().map(InventoryEntry::fromItem).toList());

            InventoryEntry preferredItem = !upcomingItems.isEmpty()
                    ? upcomingItems.get(0)
                    : inventoryItems.isEmpty() ? null : inventoryItems.get(0);
            showItemSummary(preferredItem);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Admin", "Khong the tai du lieu", e.getMessage());
        }
    }

    private List<Integer> getSelectedRequestIds() {
        return requestItems.stream()
                .filter(item -> {
                    BooleanProperty property = requestSelection.get(item.requestId());
                    return property != null && property.get();
                })
                .map(AdminService.PendingItemRequest::requestId)
                .toList();
    }

    private void showItemSummary(InventoryEntry entry) {
        if (itemname1 == null || baseprice1 == null || increment1 == null) {
            return;
        }

        if (entry == null) {
            itemname1.setText("Item name");
            baseprice1.setText("");
            increment1.setText("");
            return;
        }

        itemname1.setText(entry.name());
        baseprice1.setText(String.valueOf(entry.basePrice()));
        increment1.setText("N/A");
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private record InventoryEntry(String itemId, String name, String type, double basePrice, String description) {
        static InventoryEntry fromItem(Item item) {
            return new InventoryEntry(
                    item.getId(),
                    item.getName(),
                    item.getType(),
                    item.getPrices(),
                    item.getInfo()
            );
        }

        @Override
        public String toString() {
            return "[" + type + "] " + name + " | " + itemId + " | Base: " + basePrice + " | " + description;
        }
    }
}
