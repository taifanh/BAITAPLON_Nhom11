package models.Extra.messages;

public class Createitempayload {
    private String item_type;
    private String item_name;
    private String item_info;
    private double base_price;
    private double bid_increment;

    public Createitempayload(String item_type,String item_name ,String item_info, double base_price, double bid_increment) {
        this.item_type = item_type;
        this.item_name = item_name;
        this.item_info = item_info;
        this.base_price = base_price;
        this.bid_increment = bid_increment;
    }
    public Createitempayload(){}

    public String getItemType() {
        return item_type;
    }

    public String getItem_name(){return item_name;}

    public String getItemInfo() {
        return item_info;
    }

    public double getBasePrice() {
        return base_price;
    }

    public double getBidIncrement() {
        return bid_increment;
    }

    public void setItem_type(String item_type) {
        this.item_type = item_type;
    }

    public void setItem_name(String item_name){this.item_name=item_name;}

    public void setItem_info(String item_info) {
        this.item_info = item_info;
    }

    public void setBase_price(double base_price) {
        this.base_price = base_price;
    }

    public void setBid_increment(double bid_increment) {
        this.bid_increment = bid_increment;
    }
}
