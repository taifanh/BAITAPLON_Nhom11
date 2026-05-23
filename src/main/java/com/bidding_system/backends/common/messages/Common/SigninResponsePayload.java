package backends.common.messages.Common;

public class SigninResponsePayload {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    private String role;
    private double balance;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public double getBalance() {
        return balance;
    }

    public String getRole() {
        return role;
    }

    public SigninResponsePayload(String id, String email, String name, String phoneNumber, String password, String role, double balance) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.role = role;
        this.balance = balance;
    }
}
