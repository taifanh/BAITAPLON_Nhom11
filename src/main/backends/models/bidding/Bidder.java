package models.bidding;

import models.core.Item;
import models.accounts.User;

public class Bidder extends User {
    public Bidder(String id,String name,String phoneNumber,String Email,String password){
        super(id, name, phoneNumber, Email, password);
    }
    public void makeBill(Auction auction, Item item, double amount){
        auction.addBid(new BidTransaction(this,item,amount));
    }
}
