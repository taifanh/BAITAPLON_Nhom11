package models.accounts;

import models.bidding.CanBidding;
import models.core.Account;
import models.core.Item;
import models.items.ItemType;
import models.items.itemFactory;
import models.selling.CanSelling;

import java.util.HashSet;
import java.util.Scanner;

public class User extends Account implements CanBidding, CanSelling {
    HashSet<Item> items = new HashSet<>();
    private double balance;

    public double getBalance() {
        return balance;
    }

    public User(String id, String name, String email, String phoneNumber, String password) {
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
        items.add(itemFactory.createItem(itemType, itemName, itemPrice, itemDescription));
    }// tạo sản phẩm

    @Override
    public void sellItem(Item item) {// lúc đã giao dịch xong

        if (items.contains(item)) {
            items.remove(item);
        }
        else  {
            throw new IllegalArgumentException("Not exist item in this user");
        }
    }
}
