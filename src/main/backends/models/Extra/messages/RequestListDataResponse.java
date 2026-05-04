package models.Extra.messages;
import Database.RequestLog;
import java.util.List;

public class RequestListDataResponse {
    public String type = "REQUEST_LIST_DATA";
    public List<RequestLog.RequestRecord> requests;

    public RequestListDataResponse() {}
}