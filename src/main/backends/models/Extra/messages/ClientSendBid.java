package models.Extra.messages;

public class ClientSendBid {
    String id;
    double amount;

    public ClientSendBid() {}

    public ClientSendBid(String id, double amount) {
        this.id = id;
        this.amount = amount;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
}
