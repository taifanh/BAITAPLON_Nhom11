package models.bidding;

import models.core.Item;

import java.util.Date;

public class BidTransaction{
    private CanBidding bidder;
    private Item item;
    private double amount;
    private Date time;
    public BidTransaction(CanBidding bidder, Item item, double amount){
        this.bidder=bidder;
        this.item=item;
        this.amount=amount;
        this.time=new Date();
    }
    public double getAmount(){
        return  amount;
    }
    public CanBidding getBidder(){
        return  bidder;
    }
    public Item item(){
        return item;
    }
    public Date getTime(){
        return time;
    }
}
