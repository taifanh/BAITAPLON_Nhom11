package controllers.Server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void send(String json) {
        if (out != null && !socket.isClosed()) {
            System.out.println("[Send -> " + socket.getInetAddress() + "] " + json);
            out.println(json);
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[Received] " + line);
                handleMessage(line);
            }

        } catch (IOException e) {
            System.out.println("[ClientHandler] Disconnected: " + e.getMessage());
        } finally {
            ServerLauncher.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleMessage(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            String type = node.path("type").asText();

            switch (type) {

                case "CHAT" -> {
                    ServerLauncher.broadcast(json);
                }

                case "BID" -> {
                    ServerLauncher.broadcast(json);
                }

                default -> {
                    System.out.println("[Unknown type] " + type);
                    // vẫn broadcast để test
                    ServerLauncher.broadcast(json);
                }
            }

        } catch (Exception e) {
            System.out.println("[Parse error] " + json);
        }
    }
}