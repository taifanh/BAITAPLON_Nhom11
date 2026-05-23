package backends.common.messages.MsgBid;

public class PlaceBidPayload {
    double amount;

    public PlaceBidPayload(double amount) {
        this.amount = amount;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
}
