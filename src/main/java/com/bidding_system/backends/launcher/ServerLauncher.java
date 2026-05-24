package com.bidding_system.backends.launcher;

import com.bidding_system.backends.server.ServerApplication;

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
