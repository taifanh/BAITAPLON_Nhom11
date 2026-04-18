package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.JSON_request.PlaceBid;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class ServerLauncher {
    private final int port;
    private final ExecutorService executor;
    private final ObjectMapper mapper;

    public ServerLauncher() {
        this(9999);
    }

    public ServerLauncher(int port) {
        this.port = port;
        this.executor = Executors.newFixedThreadPool(10);
        this.mapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        new ServerLauncher().start();
    }

    public void start() {
        start(null, null);
    }

    public void start(CountDownLatch readySignal, AtomicReference<Throwable> startupError) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            signalReady(readySignal);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientSocket, mapper));
            }
        } catch (Exception e) {
            if (startupError != null && readySignal != null && readySignal.getCount() > 0) {
                startupError.set(e);
            }
            signalReady(readySignal);
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private void signalReady(CountDownLatch readySignal) {
        if (readySignal != null) {
            readySignal.countDown();
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ObjectMapper mapper;

    public ClientHandler(Socket clientSocket, ObjectMapper mapper) {
        this.clientSocket = clientSocket;
        this.mapper = mapper;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String jsonData = in.readLine();
            if (jsonData == null || jsonData.isBlank()) {
                out.println("Khong nhan duoc du lieu bid");
                return;
            }

            PlaceBid placeBid = mapper.readValue(jsonData, PlaceBid.class);
            out.println("Da nhan bid cua " + placeBid.getUsername() + ": " + placeBid.getAmount());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
