package models.Extra.messages;

public class placeBidpayload {
    double amount;

    public placeBidpayload(double amount) {
        this.amount = amount;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
}
