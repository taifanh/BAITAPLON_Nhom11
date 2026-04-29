package models.Extra.messages;

public class ReceiveMaxBidder {
    public final String type = "RECEIVE_BID";
    public ServerBidRespond maxBidder;
    public ReceiveMaxBidder() {}
    public ReceiveMaxBidder(ServerBidRespond maxBidder) {
        this.maxBidder = maxBidder;
    }
}
