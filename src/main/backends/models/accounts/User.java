package models.accounts;

import models.bidding.CanBidding;
import models.core.Account;
import models.core.Item;
import models.items.ItemType;
import models.items.itemFactory;
import models.selling.CanSelling;

import java.util.Scanner;

public class User extends Account implements CanBidding, CanSelling {
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

    @Override
    public void sellItem() {
        Scanner sc=new Scanner(System.in);
        String type=sc.nextLine();
        ItemType t=ItemType.valueOf(type);
        String name=sc.nextLine();
        double prices=sc.nextDouble();
        String info=sc.nextLine();
        Item item= itemFactory.createItem(t,name,prices,info);
        System.out.println(item.getId());
    }
}
