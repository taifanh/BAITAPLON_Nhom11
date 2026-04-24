package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

public class ServerConnection {

    private Socket socket;
    private static PrintWriter out;
    private BufferedReader in;
    private static final ObjectMapper mapper = new ObjectMapper();
    private volatile boolean running = false;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;
        startListenerThread();
    }

    // Gửi bất kỳ object nào lên server (tự serialize thành JSON)
    public static void send(Object payload) {
        try {
            String json = mapper.writeValueAsString(payload);
            out.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Đóng kết nối (gọi khi logout / tắt app)
    public void disconnect() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Vòng lặp nghe liên tục — chạy trên daemon thread
    private void startListenerThread() {
        Thread listener = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    final String message = line;
                    // Đẩy về JavaFX thread để update UI an toàn
                    Platform.runLater(() -> MessageBus.getInstance().dispatch(message));
                }
            } catch (IOException e) {
                if (running) {
                    // Mất kết nối ngoài ý muốn
                    Platform.runLater(() ->MessageBus.getInstance().dispatch("{\"type\":\"CONNECTION_LOST\"}")
                    );
                }
            }
        });
        listener.setDaemon(true); // tắt cùng với app, không giữ JVM sống
        listener.setName("ServerListener");
        listener.start();
    }
}