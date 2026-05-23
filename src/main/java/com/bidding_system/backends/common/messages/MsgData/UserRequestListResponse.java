package backends.common.messages.MsgData;

import java.util.List;

public class UserRequestListResponse {
    public String type = "USER_REQUEST_LIST_DATA";
    public List<RequestRecordDto> requests;

    public UserRequestListResponse() {
    }
}
