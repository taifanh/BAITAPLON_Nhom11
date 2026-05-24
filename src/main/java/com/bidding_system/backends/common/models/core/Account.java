package com.bidding_system.backends.common.models.core;

public class Account extends Entity {
    protected String name,phoneNumber,Email,password,role;
    public static final String ADMIN = "Admin";
    public static final String USER = "User";
    public Account(String id, String name, String Email, String phoneNumber, String password){
        this.id = id;
        this.name=name;
        this.phoneNumber=phoneNumber;
        this.Email=Email;
        this.password=password;
    }

    public Account() {
    }

    public String getName(){
        return name;
    }
    public String getId(){
        return id;
    }
    public String getPhoneNumber(){
        return phoneNumber;
    }
    public String getEmail(){
        return Email;
    }
    public String getPassword(){
        return password;
    }
    public String getRole(){return role;}
    public void setRole(String role){
        this.role = role;
    }
}
