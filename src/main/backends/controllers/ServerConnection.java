package controllers;

import models.Extra.NetworkUtils;

import java.io.*;
import java.net.Socket;

public class ServerConnection {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = false;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;
        startListenerThread();
    }

    public void sendRaw(String text) {
        out.println(text);
    }

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

    private void startListenerThread() {
        Thread listener = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("Connection lost");
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    public static void main(String[] args) {
        ServerConnection client = new ServerConnection();

        try {
            client.connect(NetworkUtils.getPrivateIP(), 9999);

            BufferedReader console = new BufferedReader(
                    new InputStreamReader(System.in));

            System.out.println("Connected. Type JSON:");

            String line;
            while ((line = console.readLine()) != null) {
                client.sendRaw(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}