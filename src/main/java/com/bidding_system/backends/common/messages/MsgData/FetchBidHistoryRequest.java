package backends.common.messages.MsgData;

public class FetchBidHistoryRequest {
    public String type = "FETCH_BID_HISTORY";
    public String auctionId;

    public FetchBidHistoryRequest() {}

    public FetchBidHistoryRequest(String auctionId) {
        this.auctionId = auctionId;
    }
}
