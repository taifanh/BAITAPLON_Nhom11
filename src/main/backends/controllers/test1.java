package controllers;

import Database.Inventory;
import Database.UserStore;
import models.accounts.Admin;
import models.core.Item;
import models.items.itemFactory;

import java.io.IOException;

public class test1 {
    public static void main(String[] args) throws IOException {
        Admin admin=Admin.creating_admin("A","abc","12345","admin");
        UserStore userStore=new UserStore();
        userStore.saveAdmin(admin);
    }
}
