package com.bidding_system.backends.common.messages.MsgBid;

public class PlaceBidFailed {
    String type = "PLACE_BID_FAILED";
    String reason;
    public PlaceBidFailed(String reason) {
        this.reason = reason;
    }
}
