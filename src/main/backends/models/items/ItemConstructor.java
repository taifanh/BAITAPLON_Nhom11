package models.items;

import models.core.Item;

public interface ItemConstructor {
    Item create(String id, String name, double price, String info);
}
