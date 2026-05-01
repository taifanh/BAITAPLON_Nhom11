package models.Extra.messages;

import java.time.Duration;
import java.time.LocalDateTime;

public class StartAuctionMessage {
    public String type = "START_AUCTION";
    public LocalDateTime endAt;
    public String itemName;
    public double startingPrice;
    public double bidIncrement;
    public StartAuctionMessage() {

    }
    public StartAuctionMessage(LocalDateTime endAt, String itemName, double startingPrice, double bidIncrement ) {
        this.endAt = endAt;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
    }
}
