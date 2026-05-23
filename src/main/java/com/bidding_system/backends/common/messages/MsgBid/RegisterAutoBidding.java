package backends.common.messages.MsgBid;

public class RegisterAutoBidding {
    public String type = "REGISTER_AUTO_BIDDING";
    public String auctionId;
    public String userId;
    public double maxBid;
    public double increment;
    public RegisterAutoBidding() {}
    public RegisterAutoBidding(String auctionId, String userId, double maxBid, double increment) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.maxBid = maxBid;
        this.increment = increment;
    }
}
