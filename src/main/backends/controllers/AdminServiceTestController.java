package controllers;

import models.accounts.Admin;
import models.accounts.User;

import static models.items.ItemType.*;

public class AdminServiceTestController {
    public static void main(String[] args) {
        User user1 = new User("User1","12345","1","1");
        User user2 = new User("User2","34567","2","2");
        user1.addItem(Electronics,"Laptop",5000,"new laptop");
        user1.addItem(Art,"Mona Lisa",900000,"new art");
        user2.addItem(Electronics,"Phone",3000,"new phone");

        // Tao 1 admin tam thoi de test, chua can qua DB.
        Admin admin = new Admin("ADMIN001", "Admin", "0123456789", "admin@gmail.com", "123456");

        try {
            admin.startAuction(0, 2, 0);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }


    }
}
