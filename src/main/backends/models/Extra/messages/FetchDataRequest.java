package models.Extra.messages;

public class FetchDataRequest {
    public String type; // "FETCH_INVENTORY" / "FETCH_REQUESTS"

    public FetchDataRequest() {}

    public FetchDataRequest(String type) {
        this.type = type;
    }
}