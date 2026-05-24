package com.bidding_system.backends.common.models.items;

import com.bidding_system.backends.common.models.core.Item;

public interface ItemConstructor {
    Item create(String id, String name, double price, String info);
}
