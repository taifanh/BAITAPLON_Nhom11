package backends.common.messages.MsgAuction;

public class AuctionStatusMessage {
    public String type = "AUCTION_STATUS";
    public String status;
    public String itemId;
    public String auctionId;
    public String sellerId;
    public long endTimeEpoch;
    public String maxBidderName;
    public String maxBidderAmount;
    public String startingPrice;

    public AuctionStatusMessage() {}
}
