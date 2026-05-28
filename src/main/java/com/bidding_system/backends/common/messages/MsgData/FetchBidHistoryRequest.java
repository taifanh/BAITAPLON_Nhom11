package com.bidding_system.backends.common.messages.MsgData;

import com.bidding_system.backends.common.messages.Common.MessageType;

public class FetchBidHistoryRequest {
    public String type = MessageType.FETCH_BID_HISTORY.getValue();
    public String auctionId;
    public String bidderId;

    public FetchBidHistoryRequest() {}

    public FetchBidHistoryRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public static FetchBidHistoryRequest forBidder(String bidderId) {
        FetchBidHistoryRequest request = new FetchBidHistoryRequest();
        request.type = MessageType.FETCH_USER_BID_HISTORY.getValue();
        request.bidderId = bidderId;
        return request;
    }
}
