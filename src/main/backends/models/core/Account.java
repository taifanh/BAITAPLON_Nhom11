package models.core;

public class Account extends Entity {
    protected String name,phoneNumber,Email,password;
    public Account(String id, String name, String Email, String phoneNumber, String password){
        this.id = id;
        this.name=name;
        this.phoneNumber=phoneNumber;
        this.Email=Email;
        this.password=password;
    }
    public String getName(){
        return name;
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

}