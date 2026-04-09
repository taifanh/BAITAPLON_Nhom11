package network.server;

import com.google.gson.Gson;

import java.io.*;
import java.net.*;

interface Observer {
    void update(Message message);
}

public class ClientHandler implements Observer, Runnable {
    private Socket clientSocket;
    private ServerConnection server;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();
    public ClientHandler(Socket clientSocket, ServerConnection server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }
    @Override
    public void run() {
        try {
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
            in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = gson.fromJson(line, Message.class);
                handleMessage(msg);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            server.removeClient(this);
        }
    }
    public void update(Message message)  {

    }
    private void handleMessage(Message msg) {

    }
    private void closeConnection() {
        try {
            if(in != null) { //tránh lỗi null pointer
                in.close();
            }
            if(out != null) {
                out.close();
            }
            if(clientSocket != null) {
                clientSocket.close(); // in out phụ thuộc vào socket
            }
        } catch(IOException ignored) {}
    }
}
