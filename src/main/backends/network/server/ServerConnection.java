package network.server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerConnection {
    private ServerSocket serverSocket;
    private final List<Observer> observers = Collections.synchronizedList(new ArrayList<>()); // list clients đang online
    private ExecutorService threadPool;
    public ServerConnection() {
        threadPool = Executors.newCachedThreadPool(); //dùng lại thread rảnh, tạo thread mới khi tất cả thread bận
    }
    public void Start(){
        try {
            serverSocket = new ServerSocket(5000); // bind port
            while(!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                observers.add(clientHandler);
                threadPool.execute(clientHandler); // đưa ClientHandler vào threads riêng để xử lý
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // Đồng bộ màn hình của tất cả client khi có thay đổi (Observer pattern)
    public void notifyObservers(Message message) {
        synchronized (observers) {
            for (Observer obs : observers) {
                obs.update(message);
            }
        }
    }
    public void removeClient(ClientHandler client) {
        observers.remove(client);
    }
    public static void main(String[] args) {
        new ServerConnection().Start();
    }
}
