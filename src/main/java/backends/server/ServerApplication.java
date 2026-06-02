package backends.server;

import backends.server.handler.BidProcessor;
import backends.server.service.AuctionService;
import backends.server.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerApplication {

    private static ExecutorService executor;

    public static void start() {
        executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(9999)) {

            AuctionService.restoreActiveAuctionsOnStartup();
            System.out.println("[Server] Started on port 9999");

            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("[Server] New connection: " + client.getInetAddress());
                executor.execute(new ClientHandler(client));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        System.out.println("[Server] Shutting down...");
        AuctionService.shutdown();
        try {
            BidProcessor.getInstance().shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("[Server] Shutdown completed.");
    }
}