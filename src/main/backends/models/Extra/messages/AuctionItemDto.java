package models.Extra.messages;

public class AuctionItemDto {
    public String id;
    public String type;
    public String name;
    public double price;
    public String info;

    public AuctionItemDto() {}
    public AuctionItemDto(String id, String type, String name, double price, String info) {
        this.id = id; this.type = type; this.name = name; this.price = price; this.info = info;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
}
