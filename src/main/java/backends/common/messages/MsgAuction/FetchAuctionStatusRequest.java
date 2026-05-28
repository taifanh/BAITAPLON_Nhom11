package backends.common.messages.MsgAuction;

public class FetchAuctionStatusRequest {
    public String type = "FETCH_AUCTION_STATUS";
    public String itemId; // Giả sử item.getId() trả về String

    public FetchAuctionStatusRequest() {}

    public FetchAuctionStatusRequest(String itemId) {
        this.itemId = itemId;
    }
}
