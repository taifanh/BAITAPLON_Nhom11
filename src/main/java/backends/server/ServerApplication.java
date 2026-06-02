package backends.server;

import backends.common.messages.Common.ServerShutdown;
import backends.server.handler.AuctionRoom;
import backends.server.service.AuctionService;
import backends.server.handler.ClientHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

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

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(new ServerShutdown("Server is shutting down"));
            AuctionRoom.getInstance().broadcast(json);
            Thread.sleep(1000);
        } catch (Exception ignored) {
        }
        AuctionRoom.getInstance().disconnectAll();
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