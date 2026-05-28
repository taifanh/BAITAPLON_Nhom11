package backends.common.models.items;

import backends.common.models.core.Item;

public interface ItemConstructor {
    Item create(String id, String name, double price, String info);
}
