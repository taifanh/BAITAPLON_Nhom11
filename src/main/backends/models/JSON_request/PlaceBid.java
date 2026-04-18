package models.JSON_request;

public class PlaceBid {
    private String id;
    private String username;
    private String amount;

    public PlaceBid() {
    }

    public PlaceBid(String id, String username, String amount) {
        this.id = id;
        this.username = username;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
