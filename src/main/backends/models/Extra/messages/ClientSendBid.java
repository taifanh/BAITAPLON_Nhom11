package models.Extra.messages;

public class ClientSendBid {
    public final String type = "PLACE_BID";
    public String id;
    public double amount;
    public String auctionId;
    public ClientSendBid() {}
    public ClientSendBid(String id, double amount) {
        this(id, amount, null);
    }
    public ClientSendBid(String id, double amount, String auctionId) {
        this.id = id;
        this.amount = amount;
        this.auctionId = auctionId;
    }
}
