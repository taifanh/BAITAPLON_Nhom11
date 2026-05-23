package backends.common.messages.MsgData;

public class FetchUserRequestsRequest {
    public String type = "FETCH_USER_REQUESTS";
    public String userId;
    public String requestType;

    public FetchUserRequestsRequest() {
    }

    public FetchUserRequestsRequest(String userId, String requestType) {
        this.userId = userId;
        this.requestType = requestType;
    }
}