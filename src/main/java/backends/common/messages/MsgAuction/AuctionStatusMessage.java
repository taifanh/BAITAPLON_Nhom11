package backends.common.messages.MsgAuction;

public class AuctionStatusMessage {
    public String type = "AUCTION_STATUS";
    public String status;
    public String itemId;
    public String auctionId;
    public double increment;
    public String sellerId;
    public long endTimeEpoch;
    public String maxBidderName;
    public double maxBidderAmount;
    public String startingPrice;
    public boolean userHasAutoBid = false;   // user này đang có auto bid không?
    public double  userMaxBid = 0;

    public AuctionStatusMessage() {}
}
