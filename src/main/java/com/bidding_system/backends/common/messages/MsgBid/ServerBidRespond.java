package backends.common.messages.MsgBid;

public class ServerBidRespond {
    public String name;
    public double amount;
    public String userId;
    public String auctionId;
    public ServerBidRespond() {}
    public ServerBidRespond(String name, double amount, String userId) {
        this.name = name;
        this.amount = amount;
        this.userId = userId;
    }
}
