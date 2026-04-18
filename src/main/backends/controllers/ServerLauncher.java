package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.stage.Stage;
import models.JSON_request.PlaceBid;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerLauncher {
    public static void start() {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println("Server started on port 9999");
            while (true) {
                Socket clientsocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientsocket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket clientsocket;

    public ClientHandler(Socket clientsocket) {
        this.clientsocket = clientsocket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientsocket.getOutputStream(), true)) {
            String jsonData = in.readLine();
            if (jsonData == null || jsonData.isBlank()) {
                out.println("Khong nhan duoc du lieu bid");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            PlaceBid placeBid = mapper.readValue(jsonData, PlaceBid.class);
            out.println("Da nhan bid cua " + placeBid.getUsername() + ": " + placeBid.getAmount());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
