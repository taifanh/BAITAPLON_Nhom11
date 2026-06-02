package backends.common.messages.Common;

public class ServerShutdown {
    public String type = "SERVER_SHUTDOWN";
    public String message;
    public ServerShutdown(String message) {
        this.message = message;
    }
}
