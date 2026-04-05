package models.accounts;

import models.bidding.CanBidding;
import models.core.Account;
import models.core.Item;
import models.items.ItemType;
import models.items.itemFactory;
import models.selling.CanSelling;

import java.util.Scanner;

public class User extends Account implements CanBidding, CanSelling {
    public User(String id,String name,String phoneNumber,String Email,String password){
        super(id, name, phoneNumber, Email, password);
    }
    public void sellItem(){
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
