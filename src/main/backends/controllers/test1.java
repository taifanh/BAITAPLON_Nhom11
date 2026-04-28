package controllers;

import Database.Inventory;
import Database.UserStore;
import models.accounts.Admin;
import models.core.Item;
import models.items.ItemType;
import models.items.itemFactory;

import java.io.IOException;

public class test1 {
    public static void main(String[] args) throws IOException {
//        Admin admin=Admin.creating_admin("A","abc","12345","admin");
//        UserStore userStore=new UserStore();
//        userStore.saveAdmin(admin);

        Inventory inventory=new Inventory();
        inventory.saveItem(itemFactory.createItem(ItemType.Art, "bao ngu", 0, "oc cak"), "USER1");
    }
}
