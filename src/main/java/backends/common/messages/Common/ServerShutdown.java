package backends.common.messages.Common;

public class ServerShutdown {
    String type = "SERVER_SHUTDOWN";
    String message;
    public ServerShutdown(String message) {
        this.message = message;
    }
}
