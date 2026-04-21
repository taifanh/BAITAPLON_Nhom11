package controllers.Server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerLauncher {

    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void start() {
        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println("[Server] Started on port 9999");

            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("[Server] New connection: " + client.getInetAddress());

                ClientHandler handler = new ClientHandler(client);
                clients.add(handler);
                executor.execute(handler);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String msg) {
        System.out.println(msg);
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }
    public static void remove(ClientHandler c) {
        clients.remove(c);
    }
    public static void main(String[] args) {
        start();
    }
}