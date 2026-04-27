package models.Extra.messages;

public class ClientSendBid {
    public final String type = "PLACE_BID";
    public String id;
    public double amount;
    public ClientSendBid() {}
    public ClientSendBid(String id, double amount) {
        this.id = id;
        this.amount = amount;
    }
}
