package backends.common.messages.MsgData;
import backends.server.database.RequestLogDAO;
import java.util.List;

public class RequestListDataResponse {
    public String type = "REQUEST_LIST_DATA";
    public List<RequestLogDAO.RequestRecord> requests;

    public RequestListDataResponse() {}
}
