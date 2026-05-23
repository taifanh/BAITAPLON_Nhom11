package com.bidding_system.backends.client.controllers.util;

import com.bidding_system.backends.common.models.core.Item;
import com.bidding_system.backends.common.models.items.ItemFactory;
import com.bidding_system.backends.common.models.items.ItemType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class ItemJsonParser {

    private ItemJsonParser() {}

    public static List<Item> parse(JsonNode arrayNode) {
        List<Item> result = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) return result;

        for (JsonNode node : arrayNode) {
            try {
                String   id           = node.path("id").asText();
                String   name         = node.path("name").asText();
                String   typeStr      = node.path("type").asText();
                double   price        = node.has("prices")
                        ? node.path("prices").asDouble()
                        : node.path("price").asDouble();
                double   bidIncrement = node.path("bidIncrement")
                        .asDouble(node.path("bid_increment").asDouble(0));
                String   info         = node.path("info").asText();

                ItemType itemType = ItemType.valueOf(typeStr);
                Item     item     = ItemFactory.createItem(itemType, name, price, info);
                item.setId(id);
                item.setBidIncrement(bidIncrement);
                result.add(item);
            } catch (Exception e) {
                System.err.println("Failed to parse item: " + e.getMessage());
            }
        }
        return result;
    }
}