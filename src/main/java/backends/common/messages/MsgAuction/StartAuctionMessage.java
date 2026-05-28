package backends.common.messages.MsgAuction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class StartAuctionMessage {
    public String type = "START_AUCTION";
    public LocalDateTime endAt;
    public String itemName;
    public String auctionId;
    public String sellerId;
    public double startingPrice;
    public double bidIncrement;
    public StartAuctionMessage() {

    }
    public StartAuctionMessage(LocalDateTime endAt, String itemName, String sellerId,  String auctionId,  double startingPrice, double bidIncrement ) {
        this.endAt = endAt;
        this.itemName = itemName;
        this.sellerId = sellerId;
        this.auctionId = auctionId;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
    }

    public StartAuctionMessage(long endTimeEpoch, String name, String sellerId, String auctionId, Double prices, double bidIncrement) {
        this.endAt = Instant.ofEpochMilli(endTimeEpoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        this.itemName = name;
        this.sellerId = sellerId;
        this.auctionId = auctionId;
        this.startingPrice = prices != null ? prices : 0.0;
        this.bidIncrement = bidIncrement;
    }
}
