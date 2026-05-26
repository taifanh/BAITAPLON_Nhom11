package com.bidding_system.backends.launcher;

import com.bidding_system.backends.common.models.accounts.Admin;
import com.bidding_system.backends.server.ServerApplication;
import com.bidding_system.backends.server.database.UserDAO;

import java.io.IOException;

public class ServerLauncher {
    public static String serverIp = "localhost";

    public static void main(String[] args) {
        ServerStart();
    }

    public static void ServerStart() {
        Admin admin = Admin.creating_admin("Admin", "Admin", "12345", "admin");
        System.out.println(admin.getPhoneNumber());
        UserDAO userDAO = new UserDAO();
        try {
            userDAO.saveAdmin(admin);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Thread serverThread = new Thread(() -> {
            System.out.println("[ServerLauncher] Khởi động Server tại IP: " + GetExactIP.getIP());
            ServerApplication.start();
        });
        serverThread.start();
    }
}
