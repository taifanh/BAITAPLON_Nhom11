package network.server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerConnection {
    private ServerSocket serverSocket;
    private List<ClientHandler> connectedClient = Collections.synchronizedList(new ArrayList<>());
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    public void Start(){
        try {
            serverSocket = new ServerSocket(5000);
            while(!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                connectedClient.add(clientHandler);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void Broadcast() {

    }
}
