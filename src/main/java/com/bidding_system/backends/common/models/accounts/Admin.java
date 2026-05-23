package com.bidding_system.backends.common.models.accounts;

import com.bidding_system.backends.server.service.AdminService;
import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.common.models.core.Account;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Admin extends Account {
    private static Admin instance;
    private final Map<String, String> userItem = new HashMap<>();

    public Admin(String id, String name, String email, String phoneNumber, String password) {
        super(id, name, email, phoneNumber,  password);
        instance = this;
    }

    private Admin(String name, String email, String phoneNumber, String password) {
        this(buildGeneratedId(phoneNumber), name, email ,phoneNumber, password);
    }
    private Admin(){}

    private static String buildGeneratedId(String phoneNumber) {
        return "ADMIN" + phoneNumber;
    }

    public static Admin getInstance() {
        if (instance==null){
            instance = new Admin();
        }
        return instance;
    }

    public static void setInstance(Admin admin) {
        instance = admin;
    }

    public static Admin creating_admin(String name, String email, String phoneNumber, String password) {
        if (instance == null) {
            instance = new Admin(name, email, phoneNumber, password);
            return instance;
        }
        // Vứt Exception ra, và Controller bên JavaFX sẽ bắt lấy để hiển thị Alert
        throw new IllegalStateException("One admin already exists.");
    }

    public void manageAuction(Auction auction) {

    }

    public void createsession(String sessionId) {

    }

    public Map<String, String> getUserItem() {
        return userItem;
    }
    public Auction startAuction(int hours,int minutes,int seconds) throws IOException {
        return AdminService.startAuction(this,hours,minutes,seconds);
    }
}
