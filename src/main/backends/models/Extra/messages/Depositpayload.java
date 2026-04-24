package models.Extra.messages;

public class Depositpayload {
    private double amount;
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {this.amount = amount;}

    public Depositpayload(){}
    public Depositpayload(double amount) {
        this.amount = amount;
    }
}
