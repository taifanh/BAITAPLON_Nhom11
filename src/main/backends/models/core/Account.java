package models.core;

public abstract class Account extends Entity{
    protected String name;
    protected String phoneNumber;
    protected String email;
    protected String password;


    public Account(){};// cần rỗng vì class con có constructor không có super

    public Account(String phoneNumber){
        super(phoneNumber);
    }

    // hàm getter và setter
    public String getName() {return name;}

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
