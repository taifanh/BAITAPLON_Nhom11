package models.items;
import models.core.Item;
import java.util.concurrent.atomic.AtomicLong;

public enum ItemType {
    // Chỉ cần khai báo thêm ở đây là xong, không cần sửa Factory hay IdGenerator nữa
    Electronics("ELE", Electronics::new),
    Art("ART", Art::new),
    Vehicle("VEH", Vehicle::new);

    private final String prefix;
    private final AtomicLong counter;
    private final ItemConstructor constructor;

    // Constructor của Enum
    ItemType(String prefix, ItemConstructor constructor) {
        this.prefix = prefix;
        this.constructor = constructor;
        this.counter = new AtomicLong(0); // Tự động tạo bộ đếm riêng cho mỗi loại
    }

    // Công thức chung để tạo ID
    public String generateId() {
        return prefix + String.format("%03d", counter.incrementAndGet());
    }

    // Công thức chung để tạo Object
    public Item create(String name, double price, String info) {
        return constructor.create(generateId(), name, price, info);
    }
}
