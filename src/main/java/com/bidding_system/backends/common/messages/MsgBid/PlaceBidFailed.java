package com.bidding_system.backends.common.messages.MsgBid;

public class PlaceBidFailed {
    public String type = "PLACE_BID_FAILED";
    public String reason;
    public PlaceBidFailed() {}
    public PlaceBidFailed(String reason) {
        this.reason = reason;
    }
}
