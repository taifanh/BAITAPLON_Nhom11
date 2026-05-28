package backends.launcher;

import backends.common.models.accounts.Admin;
import backends.server.ServerApplication;
import backends.server.database.UserDAO;

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
            if (!userDAO.phoneNumberExists(admin.getPhoneNumber()))
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
