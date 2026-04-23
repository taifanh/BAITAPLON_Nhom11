package models.accounts;

import controllers.AdminService;
import javafx.scene.control.Alert;
import models.bidding.Auction;
import models.core.Account;

import java.io.IOException;

public class Admin extends Account {
    private static Admin instance;

    public Admin(String id, String name, String phoneNumber, String email, String password) {
        super(name, phoneNumber, email, password);
        this.id = id;
    }

    private Admin(String name, String phoneNumber, String email, String password) {
        super(name, phoneNumber, email, password);
    }

    public void creating_admin(String name, String phoneNumber, String email, String password) {
        if (instance == null) {
            instance = new Admin(name, phoneNumber, email, password);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("admin");
        alert.setHeaderText("admin register error");
        alert.setContentText("one admin existed");
        alert.showAndWait();
    }

    public Auction startAuction(int hours, int minutes, int seconds) {
        try {
            return AdminService.startAuction(this, hours, minutes, seconds);
        } catch (IOException e) {
            throw new RuntimeException("Khong the bat dau phien dau gia", e);
        }
    }

    public Auction StartAuction(int hours, int minutes, int seconds) {
        return startAuction(hours, minutes, seconds);
    }

    public void manageAuction(Auction auction) {}

    public void createsession(String s) {
    }
}
