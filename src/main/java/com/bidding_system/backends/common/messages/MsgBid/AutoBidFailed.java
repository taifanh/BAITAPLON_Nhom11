package com.bidding_system.backends.common.messages.MsgBid;

public class AutoBidFailed {
    public String type = "PLACE_BID_FAILED";
    public String reason;
    public AutoBidFailed() {}
    public AutoBidFailed(String reason) {
        this.reason = reason;
    }
}
