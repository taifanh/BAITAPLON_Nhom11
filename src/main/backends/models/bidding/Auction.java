package models.bidding;

import models.core.Item;

import java.util.ArrayList;
import  java.util.Date;
import java.util.List;

public class Auction {
    private ArrayList<Item> Items=new ArrayList<Item>();
    private ArrayList<BidTransaction> Bids=new ArrayList<BidTransaction>();
    private Date start;
    private Date end;
    public Auction(){
        Items=new ArrayList<>();
        Bids=new ArrayList<>();
    }
    public void addItem(Item item) {
        Items.add(item);
    }
    public void addBid(BidTransaction bid){
        Bids.add(bid);
    }
    public void start(Date time){
        this.start=time;
    }
    public void end(Date time){
        this.end=time;
    }
    public List<Item> getItemsList(){
        return Items;
    }
    public List<BidTransaction> getBidList(){
        return Bids;
    }
}
