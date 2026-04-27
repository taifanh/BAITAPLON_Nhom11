package models.accounts;

import Database.Inventory;
import controllers.AuctionService;
import models.bidding.Auction;
import models.bidding.CanBidding;
import models.core.Account;
import models.core.Item;
import models.items.ItemType;
import models.items.itemFactory;
import models.selling.CanSelling;

import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;

public class User extends Account implements CanBidding, CanSelling {
    HashSet<Item> items = new HashSet<>();
    private double balance;

    public double getBalance() {
        return balance;
    }
    public void setBalance(double balance){
        this.balance = balance;
    }

    public User(String id, String name, String email, String phoneNumber, String password , double balance) {
        super(id, name, email,phoneNumber, password);
        this.balance = balance;

    }
    public User(String id, String name, String email, String phoneNumber, String password) {
        super(id, name, email,phoneNumber,  password);

    }

    public User(String name, String email, String phoneNumber, String password) {
        this(buildGeneratedId(phoneNumber), name, email,phoneNumber,  password);
        this.balance = 0.0;

    }

    private static String buildGeneratedId(String phoneNumber) {
        String normalizedPhoneNumber = phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");
        if (normalizedPhoneNumber.isBlank()) {
            return "USER";
        }
        return "USER" + normalizedPhoneNumber;
    }

    public void deposit(double amount) {

        this.balance += amount;

    }

    public void withdraw(double amount) {
        this.balance -= amount;
    }

    //tao san pham
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
    public void sellItem(Item item) {// lúc đã giao dịch xong

        if (items.contains(item)) {
            items.remove(item);
        }
        else  {
            throw new IllegalArgumentException("Not exist item in this user");
        }
    }
    public void bids(Auction auction,double amount) throws IOException {
        AuctionService.placeBid(this,auction,amount);
    }
}