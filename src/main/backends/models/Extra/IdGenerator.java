package models.Extra;

import models.items.ItemType;

import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {

    private static final AtomicLong electronicsCounter = new AtomicLong(0);
    private static final AtomicLong artCounter = new AtomicLong(0);
    private static final AtomicLong vehicleCounter = new AtomicLong(0);

    public static long nextId(ItemType type) {
        switch (type) {
            case Electronics:
                return electronicsCounter.incrementAndGet();
            case Art:
                return artCounter.incrementAndGet();
            case Vehicle:
                return vehicleCounter.incrementAndGet();
            default:
                throw new IllegalArgumentException("Invalid type");
        }
    }
}