package backends.common.messages.MsgData;

import backends.common.messages.Common.MessageType;

public class FetchBuyerToAuctionRequest {
    public String UserId;
    public String type = MessageType.FETCH_BUYER_ITEM.getValue();
    public String requestType ;

    public FetchBuyerToAuctionRequest() {
        this.type = MessageType.FETCH_BUYER_ITEM.getValue();
    }

    public FetchBuyerToAuctionRequest(String UserId, String requestType) {
        this.UserId = UserId;
        this.requestType = requestType;
        this.type = MessageType.FETCH_BUYER_ITEM.getValue();
    }
}

