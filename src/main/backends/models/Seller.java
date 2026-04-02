package com.example.model;

public class Seller extends User{
    public Seller(String id,String name,String phoneNumber,String Email,String password){
        super(id, name, phoneNumber, Email, password);
    }
    public void addItem(Auction auction,Item item){
        auction.addItem(item);
    }
}
