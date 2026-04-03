package main.backends.models.items;

import models.core.Item;

//Dựa vào tham số type được nhập vào mà hàm createItem(Factory pattern) sẽ return object kiểu sản phẩm tương ứng
public class itemFactory {
    public static Item createItem(ItemType type, String id, String name, double price, String info) {
        return switch (type) {
            case Electronics -> new Electronics(id, name, price, info);
            case Art -> new Art(id, name, price, info);
            case Vehicle -> new Vehicle(id, name, price, info);
            default -> throw new IllegalArgumentException("This item type is not available now: " + type);
            //default để đảm bảo nếu type không khớp với 3 trường hợp trên thì ném ra lỗi
        };
    }
}
