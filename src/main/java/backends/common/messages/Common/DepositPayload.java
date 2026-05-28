package backends.common.messages.Common;

public class DepositPayload {
    private double amount;
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {this.amount = amount;}

    public DepositPayload(){}
    public DepositPayload(double amount) {
        this.amount = amount;
    }
}
