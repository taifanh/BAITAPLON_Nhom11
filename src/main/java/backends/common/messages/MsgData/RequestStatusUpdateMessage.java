package backends.common.messages.MsgData;

public class RequestStatusUpdateMessage {
    public String type = "REQUEST_STATUS_UPDATED";
    public String requestId;
    public String status;
    public String itemId;
    public String sellerId;

    public RequestStatusUpdateMessage() {
    }

    public RequestStatusUpdateMessage(String requestId, String status, String itemId) {
        this.requestId = requestId;
        this.status = status;
        this.itemId = itemId;
    }
}
