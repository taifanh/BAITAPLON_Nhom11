package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

public class ServerConnection {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile boolean running = false;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;
        startListenerThread();
    }

    // Gửi bất kỳ object nào lên server (tự serialize thành JSON)
    public void send(Object payload) {
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
            if (in != null) in.close();
            if (out != null) out.close();
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
        listener.setDaemon(true);
        listener.setName("ServerListener");
        listener.start();
    }
    public static void main(String[] args) {
        ServerConnection client = new ServerConnection();

        try {
            client.connect("10.11.71.187", 9999);
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Connected");
            String line;
            while ((line = console.readLine()) != null) {
                client.send(line); // gửi thẳng string
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}