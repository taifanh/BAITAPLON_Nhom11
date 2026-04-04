package models.accounts;

import models.core.Entity;

public class User extends Entity {
    protected String name;
    protected String phoneNumber;
    protected String email;
    protected String password;


    // constructor khi load tưf file
    public User(String id,String name,String phoneNumber,String email,String password){
        super(id);
        this.name=name;
        this.phoneNumber=phoneNumber;
        this.email=email;
        this.password=password;
    }

    // constructor khi mới đăng kí
    public User(String name, String email, String phoneNumber, String password) {
        this(phoneNumber, name, phoneNumber, email, password);
    }

    public String getName() {return name;}

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
