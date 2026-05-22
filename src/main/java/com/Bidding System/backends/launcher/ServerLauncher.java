package backends.launcher;

import backends.server.ServerApplication;
import backends.launcher.GetExactIP;

public class ServerLauncher {
    public static String serverIp = "localhost";

    public static void main(String[] args) {
        ServerStart();
    }

    public static void ServerStart() {
        Thread serverThread = new Thread(() -> {
            System.out.println("[ServerLauncher] Khởi động Server tại IP: " + GetExactIP.getIP());
            ServerApplication.start();
        });
        serverThread.start();
    }
}
