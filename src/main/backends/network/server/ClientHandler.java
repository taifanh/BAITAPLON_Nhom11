package network.server;

import java.net.*;

public class ClientHandler implements Runnable {
    Socket clientScoket;
    public ClientHandler(Socket clientScoket) {
        this.clientScoket = clientScoket;
    }

    @Override
    public void run() {

    }
}
