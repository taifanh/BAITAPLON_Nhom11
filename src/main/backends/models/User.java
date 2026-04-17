package models;

public abstract class User extends Entity {
    protected String name,phoneNumber,Email,password;
    public User(String id,String name,String phoneNumber,String Email,String password){
        super(id);
        this.name=name;
        this.phoneNumber=phoneNumber;
        this.Email=Email;
        this.password=password;
    }
    public String getName() {return name;}

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        this.Email = email;
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
