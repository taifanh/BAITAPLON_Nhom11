package models.accounts;

import javafx.scene.control.Alert;
import models.bidding.Auction;
import models.core.Account;

import java.util.HashMap;
import java.util.Map;

public class Admin extends Account {
    private static Admin instance;
    private final Map<String, String> userItem = new HashMap<>();

    public Admin(String id, String name, String phoneNumber, String email, String password) {
        super(id, name, phoneNumber, email, password);
        instance = this;
    }

    private Admin(String name, String phoneNumber, String email, String password) {
        this(buildGeneratedId(phoneNumber), name, phoneNumber, email, password);
    }

    private static String buildGeneratedId(String phoneNumber) {
        return "ADMIN" + phoneNumber;
    }

    public static Admin getInstance() {
        return instance;
    }

    public static void setInstance(Admin admin) {
        instance = admin;
    }

    public Admin creating_admin(String name, String phoneNumber, String email, String password) {
        if (instance == null) {
            instance = new Admin(name, phoneNumber, email, password);
            return instance;
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Admin");
        alert.setHeaderText("Admin register error");
        alert.setContentText("One admin already exists.");
        alert.showAndWait();
        return instance;
    }

    public void manageAuction(Auction auction) {

    }

    public void createsession(String sessionId) {

    }

    public Map<String, String> getUserItem() {
        return userItem;
    }
}
