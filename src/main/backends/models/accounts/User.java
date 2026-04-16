package models.accounts;

import Database.Inventory;
import models.bidding.CanBidding;
import models.core.Account;
import models.core.Item;
import models.items.ItemType;
import models.items.itemFactory;
import models.selling.CanSelling;

import java.io.IOException;
import java.util.HashSet;

public class User extends Account implements CanBidding, CanSelling {
    HashSet<Item> items = new HashSet<>();
    private double balance;

    public double getBalance() {
        return balance;
    }

    public User(String id, String name, String phoneNumber, String email, String password) {
        super(id, name, phoneNumber, email, password);
    }

    public User(String name, String email, String phoneNumber, String password) {
        this(buildGeneratedId(phoneNumber), name, phoneNumber, email, password);
    }

    private static String buildGeneratedId(String phoneNumber) {
        StringBuilder builder = new StringBuilder("USER");
        for (int i = 1; i < phoneNumber.length(); i++) {
            builder.append(phoneNumber.charAt(i) - 1);
        }
        return builder.toString();
    }

    public void deposit(double amount) {
        this.balance += amount;
    }

    public void withdraw(double amount) {
        this.balance -= amount;
    }

    public void addItem(ItemType itemType, String itemName, double itemPrice, String itemDescription) {
        Item item = itemFactory.createItem(itemType, itemName, itemPrice, itemDescription);
        try {
            Inventory inventory = new Inventory();
            inventory.saveItem(item, id);
            items.add(item);
        } catch (IOException e) {
            System.out.println("Khong the tao san pham");
        }
    }

    @Override
    public void sellItem(Item item) {

        if (items.contains(item)) {
            items.remove(item);
        }
        else  {
            throw new IllegalArgumentException("Not exist item in this user");
        }
    }
}
