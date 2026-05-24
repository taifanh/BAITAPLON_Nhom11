package com.bidding_system.backends.common.messages.MsgData;

import java.util.List;

public class BidHistoryDataResponse {
    public String type = "BID_HISTORY_DATA";
    public String auctionId;
    public List<BidHistoryRecordDto> records;

    public BidHistoryDataResponse() {}
}
