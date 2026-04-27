package controllers;

import Database.Auctions;
import Database.BidTransactions;
import Database.Inventory;
import Database.UserStore;
import Database.request_log;
import models.accounts.Admin;
import models.accounts.User;
import models.bidding.Auction;

import java.io.IOException;
import java.nio.file.Path;

import static models.items.ItemType.Art;
import static models.items.ItemType.Electronics;

public class test1 {
    public static void main(String[] args) {
        User user1 = new User("User1", "12345", "1", "1");
        User user2 = new User("User2", "34567", "2", "2");
        User user3 = new User("User3", "123", "3", "3");
        ;

//        user1.addItem(Electronics, "Laptop", 5000, "new laptop");
//        user1.addItem(Art, "Mona Lisa", 900000, "new art");
//        user2.addItem(Electronics, "Phone", 3000, "new phone");

        Admin admin = new Admin("ADMIN001", "Admin", "0123456789", "admin@gmail.com", "123456");

        try {
            Auction auction = admin.startAuction(0, 0, 10);
            user2.bids(auction, 2000);
            Thread.sleep(1000);
            user3.bids(auction, 3000);
            Thread.sleep(1000);
            user2.bids(auction, 4000);
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
