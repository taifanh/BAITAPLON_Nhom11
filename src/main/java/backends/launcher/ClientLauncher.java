package backends.launcher;

import backends.client.ClientApplication;
import javafx.application.Application;

public class ClientLauncher {
    public static String serverIp = "localhost";
    public static void main(String[] args) {
        ClientStart(args);
    }

    public static void ClientStart(String[] args) {
        Thread clientThread = new Thread(() -> {
            System.out.println("[ClientLauncher] Khởi động Client tại IP: " + GetExactIP.getIP());
            Application.launch(ClientApplication.class, args);
        });
        clientThread.start();
    }
}
