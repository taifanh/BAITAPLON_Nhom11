package backends.common.messages.MsgAuction;


public class AuctionResultMessage {
    public AuctionResultMessage() {

    }
    public String type = "AUCTION_RESULT";
    public String itemId;
    public String itemName;
    public String winnerId;
    public String winnerName;
    public double winningAmount;
    public boolean hasBidder; // false = UNSOLD
}
