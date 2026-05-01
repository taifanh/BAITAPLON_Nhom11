package models.Extra.messages;

public class ServerBidRespond {
    public String name;
    public double amount;
    public ServerBidRespond() {}
    public ServerBidRespond(String name, double amount) {
        this.name = name;
        this.amount = amount;
    }
}