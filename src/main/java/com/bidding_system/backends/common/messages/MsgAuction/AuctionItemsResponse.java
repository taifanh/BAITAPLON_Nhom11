package backends.common.messages.MsgAuction;

import backends.common.messages.MsgAuction.AuctionItemDto;

import java.util.List;


public class AuctionItemsResponse {
    public String type = "AUCTION_ITEMS_RESPONSE";
    public List<AuctionItemDto> items;

    public AuctionItemsResponse() {}
    public AuctionItemsResponse(List<AuctionItemDto> items) {
        this.items = items;
    }
}
