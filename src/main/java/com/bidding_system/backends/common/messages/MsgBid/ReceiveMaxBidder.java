package backends.common.messages.MsgBid;

public class ReceiveMaxBidder {
    public String type = "RECEIVE_BID";
    public backends.common.messages.MsgBid.ServerBidRespond maxBidder;
    public ReceiveMaxBidder() {}
    public ReceiveMaxBidder(ServerBidRespond maxBidder) {
        this.maxBidder = maxBidder;
    }
}
