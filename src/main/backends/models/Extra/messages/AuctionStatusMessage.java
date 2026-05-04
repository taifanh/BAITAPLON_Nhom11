package models.Extra.messages;

public class AuctionStatusMessage {
    public String type = "AUCTION_STATUS";
    public String status;
    public String itemId;
    public String auctionId;
    public long endTimeEpoch;

    public AuctionStatusMessage() {}
}