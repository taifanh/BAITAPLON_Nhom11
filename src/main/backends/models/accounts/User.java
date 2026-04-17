package models.accounts;

import Database.Inventory;
import Database.UserStore;
import models.bidding.CanBidding;
import models.core.Account;
import models.core.Item;
import models.items.ItemType;
import models.items.itemFactory;
import models.selling.CanSelling;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class User extends Account implements CanBidding, CanSelling {
    private final Set<Item> items = new HashSet<>();
    private double balance;

    public User() {}

    public User(String id, String name, String phoneNumber, String email, String password) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.password = password;
    }

    public User(String name, String phoneNumber, String email, String password) {
        super(name, phoneNumber, email, password);
        this.id = generateEntity();
    }

    public double getBalance() {
        return balance;
    }

    public String generateEntity() {
        Random random = new Random();
        UserStore userStore = new UserStore();
        String generatedId;

        do {
            generatedId = "USER" + (100000 + random.nextInt(899999));
        } while (isExistingId(userStore, generatedId));

        return generatedId;
    }

    private boolean isExistingId(UserStore userStore, String id) {
        try {
            return userStore.IDexist(id);
        } catch (IOException e) {
            throw new RuntimeException("Khong the kiem tra ID da ton tai hay chua.", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deposit(double amount) {
        balance += amount;
    }

    public void withdraw(double amount) {
        balance -= amount;
    }

    public void addItem(ItemType itemType, String itemName, double itemPrice, String itemDescription) {
        Item item = itemFactory.createItem(itemType, itemName, itemPrice, itemDescription);
        try {
            Inventory inventory = new Inventory();
            inventory.saveItem(item, getId());
            items.add(item);
        } catch (IOException e) {
            throw new RuntimeException("Khong the tao san pham", e);
        }
    }

    @Override
    public void sellItem(Item item) {
        if (!items.remove(item)) {
            throw new IllegalArgumentException("Not exist item in this user");
        }
    }
}
