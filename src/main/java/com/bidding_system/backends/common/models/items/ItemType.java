package backends.common.models.items;

import backends.common.Extra.IdGenerator;
import backends.common.models.core.Entity;
import backends.common.models.core.Item;

public enum ItemType {
    Electronics("ELE", Electronics::new),
    Art("ART", Art::new),
    Vehicle("VEH", Vehicle::new);

    private final String prefix;
    private final ItemConstructor constructor;

    ItemType(String prefix, ItemConstructor constructor) {
        this.prefix = prefix;
        this.constructor = constructor;
    }

    public String generateId() {
        return prefix + Entity.makeItemId(IdGenerator.nextId(this));
    }

    public Item create(String name, double price, String info) {
        return constructor.create(generateId(), name, price, info);
    }
}
