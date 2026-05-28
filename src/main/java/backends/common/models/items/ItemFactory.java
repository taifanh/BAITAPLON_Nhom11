package backends.common.models.items;

import backends.common.models.core.Item;

// Dựa vào tham số type được nhập vào mà hàm createItem (Factory pattern) sẽ return object kiểu sản phẩm tương ứng
public class ItemFactory {
    public static Item createItem(ItemType type, String name, double price, String info) {
        return type.create(name, price, info);
    }

    public static Item createItem(ItemType type, String name, double price, String info, double bidIncrement) {
        Item item = type.create(name, price, info);
        item.setBidIncrement(bidIncrement);
        return item;
    }
}
