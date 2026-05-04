package models.Extra.messages;
import models.core.Item;
import java.util.List;

public class InventoryDataResponse {
    public String type = "INVENTORY_DATA";
    public List<Item> waitingItems;
    public List<Item> scheduledItems;
    public List<Item> inProgressItems;

    public InventoryDataResponse() {}
}