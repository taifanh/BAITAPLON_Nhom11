package com.bidding_system.backends.common.messages.MsgData;
import com.bidding_system.backends.server.database.RequestLogDAO;
import java.util.List;

public class RequestListDataResponse {
    public String type = "REQUEST_LIST_DATA";
    public List<RequestLogDAO.RequestRecord> requests;

    public RequestListDataResponse() {}
}
