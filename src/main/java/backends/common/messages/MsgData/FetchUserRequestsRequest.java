package backends.common.messages.MsgData;

import backends.common.messages.Common.MessageType;

public class FetchUserRequestsRequest {
    public String type = MessageType.FETCH_USER_REQUEST.getValue();
    public String userId;
    public String requestType;


    public FetchUserRequestsRequest(){
    }
    public FetchUserRequestsRequest(String userId, String requestType) {
        this.userId = userId;
        this.requestType = requestType;
    }
}
