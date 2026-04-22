package models.Extra;

import models.items.ItemType;

public class IdGenerator {
    private static long lastGeneratedId = System.currentTimeMillis();

    private IdGenerator() {
    }

    public static synchronized long nextId() {
        return nextUniqueId();
    }

    public static synchronized long nextId(ItemType type) {
        if (type == null) {
            throw new IllegalArgumentException("Item type is required");
        }
        return nextUniqueId();
    }

    private static long nextUniqueId() {
        long now = System.currentTimeMillis();
        if (now <= lastGeneratedId) {
            lastGeneratedId++;
        } else {
            lastGeneratedId = now;
        }
        return lastGeneratedId;
    }
}
