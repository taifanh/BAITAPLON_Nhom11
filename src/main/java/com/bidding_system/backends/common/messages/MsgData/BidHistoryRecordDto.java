package com.bidding_system.backends.common.messages.MsgData;

import java.time.Instant;

public class BidHistoryRecordDto {
    public String auctionId;
    public String bidderId;
    public String bidderName;
    public String itemId;
    public double amount;
    public Instant bidTime;

    public BidHistoryRecordDto() {}

    public BidHistoryRecordDto(
            String auctionId,
            String bidderId,
            String bidderName,
            String itemId,
            double amount,
            Instant bidTime
    ) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.itemId = itemId;
        this.amount = amount;
        this.bidTime = bidTime;
    }
}
