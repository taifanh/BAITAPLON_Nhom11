package com.bidding_system.backends.client.controllers.components;

public class BidHistoryRow {
    private final String sequence;
    private final String bidder;
    private final String amount;
    private final String time;

    public BidHistoryRow(String sequence, String bidder, String amount, String time) {
        this.sequence = sequence;
        this.bidder = bidder;
        this.amount = amount;
        this.time = time;
    }

    public String getSequence() {
        return sequence;
    }

    public String getBidder() {
        return bidder;
    }

    public String getAmount() {
        return amount;
    }

    public String getTime() {
        return time;
    }
}
