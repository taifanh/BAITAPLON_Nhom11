package models.accounts;

import models.bidding.Auction;
import models.core.Account;

public class Admin extends Account {
    public Admin(String id,String name,String phoneNumber,String Email,String password){
        super(id, name, phoneNumber, Email, password);
    }
    public void manageAuction(Auction auction){

    }
}
