package models.Extra.messages;

public class Createitempayload {
    private String item_info;
    private double base_price;
    private double bid_increment;

    public Createitempayload(String item_info, double base_price, double bid_increment) {
        this.item_info = item_info;
        this.base_price = base_price;
        this.bid_increment = bid_increment;
    }


}
