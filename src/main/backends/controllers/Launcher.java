package controllers;

import javafx.application.Application;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Launcher {
    public static void main(String[] args) {
        CountDownLatch serverReady = new CountDownLatch(1);
        AtomicReference<Throwable> startupError = new AtomicReference<>();
        ServerLauncher serverLauncher = new ServerLauncher();

        Thread serverThread = new Thread(() -> serverLauncher.start(serverReady, startupError), "bid-server");
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            serverReady.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bi gian doan khi cho server khoi dong", e);
        }

        Throwable serverError = startupError.get();
        if (serverError != null) {
            throw new IllegalStateException("Khong the khoi dong server", serverError);
        }

        Application.launch(ClientLauncher.class, args);
    }
}
