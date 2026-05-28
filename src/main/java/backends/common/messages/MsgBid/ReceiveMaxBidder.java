package backends.common.messages.MsgBid;

public class ReceiveMaxBidder {
    public String type = "RECEIVE_BID";
    public ServerBidRespond maxBidder;
    public double currentIncrement;
    public ReceiveMaxBidder() {}
    public ReceiveMaxBidder(ServerBidRespond maxBidder, double currentIncrement) {
        this.maxBidder = maxBidder;
        this.currentIncrement = currentIncrement;
    }
}
