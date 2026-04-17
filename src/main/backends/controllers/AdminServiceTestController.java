package controllers;

import models.accounts.Admin;
import models.accounts.User;

import static models.items.ItemType.*;

public class AdminServiceTestController {
    public static void main(String[] args) {
        User user = new User("1","1","1","1");
        user.addItem(Electronics,"Laptop",5000,"new laptop");
        user.addItem(Art,"Mona Lisa",900000,"new art");
        user.addItem(Electronics,"Phone",3000,"new phone");

        // Tao 1 admin tam thoi de test, chua can qua DB.
        Admin admin = new Admin("ADMIN001", "Admin", "0123456789", "admin@gmail.com", "123456");

        try {
            admin.startAuction(0, 2, 0);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
