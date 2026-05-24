package com.bidding_system.backends.common.messages.MsgData;

public class FetchBidHistoryRequest {
    public String type = "FETCH_BID_HISTORY";
    public String auctionId;
    public String bidderId;

    public FetchBidHistoryRequest() {}

    public FetchBidHistoryRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public static FetchBidHistoryRequest forBidder(String bidderId) {
        FetchBidHistoryRequest request = new FetchBidHistoryRequest();
        request.type = "FETCH_USER_BID_HISTORY";
        request.bidderId = bidderId;
        return request;
    }
}
