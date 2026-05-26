package com.bidding_system.backends.common.messages.MsgBid;

public class AutoBiddingCancelled {
    public String type = "AUTO_BID_CANCELLED";
    public String message;
    public AutoBiddingCancelled(String message) {
        this.message = message;
    }
}
