package models.Extra.messages;

public class AuctionCommandMessage {
    public String type = "AUCTION_COMMAND";
    public String command;
    public String itemId;
    public int durationMinutes;

    public AuctionCommandMessage() {}

    public AuctionCommandMessage(String command, String itemId, int durationMinutes) {
        this.command = command;
        this.itemId = itemId;
        this.durationMinutes = durationMinutes;
    }
}