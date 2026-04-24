package controllers.Server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApplication {

    public static void start() {
        // CachedThreadPool tốt hơn FixedThreadPool cho I/O-bound:
        // tự tạo thêm thread khi cần, tự thu hồi khi nhàn rỗi
        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(9999)) {
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
}