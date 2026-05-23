package backends.launcher;

import backends.client.AdminApplication;
import backends.client.ClientApplication;
import backends.server.ServerApplication;
import javafx.application.Application;

import java.util.Scanner;

public class Launcher {
    public static String serverIp = "localhost";

    public static void main(String[] args) {
        String input = "";
        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Nhap dia chi IP Server (nhan Enter de dung localhost): ");
            if (sc.hasNextLine()) {
                input = sc.nextLine().trim();
            }
        } catch (Exception e) {
            input = "";
        }

        if (isValidHost(input)) {
            serverIp = input;
        } else {
            serverIp = "localhost";
        }

        if ("localhost".equals(serverIp)) {
            ServerStart();
            ClientStart(args);
        } else {
            ClientStart(args);
        }
    }

    public static void ServerStart() {
        Thread serverThread = new Thread(() -> {
            System.out.println("[Launcher] Dang khoi dong Server...");
            ServerApplication.start();
        });
        serverThread.start();

        try {
            System.out.println("[Launcher] Doi Server san sang...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void ClientStart(String[] args) {
        Thread clientThread = new Thread(() -> {
            System.out.println("[Launcher] Dang khoi dong Client...");
            Application.launch(ClientApplication.class, args);
        });
        clientThread.start();
    }

    public static void AdminStart(String[] args) {
        Thread clientThread = new Thread(() -> {
            System.out.println("[Launcher] Dang khoi dong Client...");
            Application.launch(AdminApplication.class, args);
        });
        clientThread.start();
    }

    private static boolean isValidHost(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.matches("[a-zA-Z0-9._-]+");
    }
}
