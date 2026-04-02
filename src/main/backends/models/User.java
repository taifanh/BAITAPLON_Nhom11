package com.example.model;

public abstract class User extends Entity {
    protected String name,phoneNumber,Email,password;
    public User(String id,String name,String phoneNumber,String Email,String password){
        super(id);
        this.name=name;
        this.phoneNumber=phoneNumber;
        this.Email=Email;
        this.password=password;
    }
    public String getName(){
        return name;
    }
}
