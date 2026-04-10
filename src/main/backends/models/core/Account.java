package models.core;

public class Account extends Entity {
    protected String name,phoneNumber,email,password;

    public Account(){}

    public Account( String name, String phoneNumber, String Email, String password){
        this.name=name;
        this.phoneNumber=phoneNumber;
        this.email=Email;
        this.password=password;
    }
    public String getName(){
        return name;
    }
    public String getPhoneNumber(){
        return phoneNumber;
    }
    public String getEmail(){
        return email;
    }
    public String getPassword(){
        return password;
    }

}
