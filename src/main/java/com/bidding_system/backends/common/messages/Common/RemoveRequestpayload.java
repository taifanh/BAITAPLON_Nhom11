package backends.common.messages.Common;

public class RemoveRequestpayload {
    private String request_id;
    private String status;

    public String getRequest_id() {
        return request_id;
    }
    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RemoveRequestpayload(String request_id, String status) {
        this.request_id = request_id;
        this.status = status;
    }
    public  RemoveRequestpayload() {
    }
}
