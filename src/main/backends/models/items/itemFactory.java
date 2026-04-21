package models.items;

import models.core.Item;

//Dựa vào tham số type được nhập vào mà hàm createItem(Factory pattern) sẽ return object kiểu sản phẩm tương ứng
public class itemFactory {
    public static Item createItem(ItemType type, String name, double price, String info) {
        // Không switch-case, không check type thủ công
        return type.create(name, price, info);
    }
}
