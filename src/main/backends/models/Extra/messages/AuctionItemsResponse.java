package models.Extra.messages;

import models.core.Item;
import java.util.List;


public class AuctionItemsResponse {
    public String type = "AUCTION_ITEMS_RESPONSE";
    public List<AuctionItemDto> items;

    public AuctionItemsResponse() {}
    public AuctionItemsResponse(List<AuctionItemDto> items) {
        this.items = items;
    }
}