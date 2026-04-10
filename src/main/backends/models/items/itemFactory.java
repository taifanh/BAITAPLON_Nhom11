package models.items;

import models.core.Item;

//Dựa vào tham số type được nhập vào mà hàm createItem(Factory pattern) sẽ return object kiểu sản phẩm tương ứng
public class itemFactory {
    public static Item createItem(ItemType type, String name, double price, String info) {
        return switch (type) {
            case Electronics -> new Electronics(Item.addId(type), name, price, info);
            case Art -> new Art(Item.addId(type), name, price, info);
            case Vehicle -> new Vehicle(Item.addId(type), name, price, info);
            default -> throw new IllegalArgumentException("Invalid type in itemFactory.class");
            //default để đảm bảo nếu type không khớp với 3 trường hợp trên thì ném ra lỗi
        };
    }
}
