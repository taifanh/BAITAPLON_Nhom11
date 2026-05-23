package backends.common.messages.MsgData;

import com.fasterxml.jackson.annotation.JsonAlias;

public class RequestRecordDto {
    @JsonAlias("id")
    public String requestId;
    public String userId;
    public String requestType;
    public String requestInfo;
    public String time;
    public String status;

    public RequestRecordDto() {
    }
}
