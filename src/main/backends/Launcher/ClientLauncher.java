package Launcher;

import controllers.AdminApplication;
import controllers.ClientApplication;
import controllers.Server.ServerApplication;
import javafx.application.Application;
import models.accounts.Admin;

import java.util.Scanner;

public class ClientLauncher {
    public static String serverIp = "localhost";
    public static void main(String[] args) {
        ClientStart(args);
    }

    public static void ServerStart() {
        Thread serverThread = new Thread(() -> {
            System.out.println("[Launcher] Đang khởi động Server...");
            ServerApplication.start();
        });
        serverThread.start();

        try {
            System.out.println("[Launcher] Đợi Server sẵn sàng...");
            Thread.sleep(2000); // Đợi 2 giây
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void ClientStart(String[] args) {
        Thread clientThread = new Thread(() -> {
            System.out.println("[Launcher] Đang khởi động Client...");
            // Gọi phương thức main của ClientLauncher
            Application.launch(ClientApplication.class, args);
        });
        clientThread.start();
    }
    public static void AdminStart(String[] args) {
        Thread clientThread = new Thread(() -> {
            System.out.println("[Launcher] Đang khởi động Client...");
            // Gọi phương thức main của ClientLauncher
            Application.launch(AdminApplication.class, args);
        });
        clientThread.start();
    }
}
