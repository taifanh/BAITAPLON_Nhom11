package backends.common.models.core;

import backends.common.Extra.IdGenerator;
import backends.common.models.accounts.User;
import backends.common.models.items.ItemType;

public abstract class Item extends Entity {
    protected String name;
    protected double prices;
    protected String info;
    protected double bidIncrement;

    public Item(String id, String name, double prices, String info) {
        this.id = id;
        this.name = name;
        this.prices = prices;
        this.info = info;
    }

    public String getName() {
        return name;
    }

    public Double getPrices() {
        return prices;
    }

    public String getInfo() {
        return info;
    }

    public double getBidIncrement() {
        return bidIncrement;
    }

    public void setBidIncrement(double bidIncrement) {
        this.bidIncrement = bidIncrement;
    }

    public String getType() {
        return getClass().getSimpleName();
    }

    public static String addId(ItemType type) {
        String prefix = switch (type) {
            case Electronics -> "E";
            case Vehicle -> "V";
            case Art -> "A";
        };
        return prefix + makeItemId(IdGenerator.nextId(type));
    }
}
