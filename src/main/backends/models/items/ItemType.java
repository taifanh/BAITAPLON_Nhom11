package models.items;

import models.Extra.IdGenerator;
import models.core.Entity;
import models.core.Item;

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
