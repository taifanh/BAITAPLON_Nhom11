package backends.common.models.accounts;

import backends.common.models.core.Account;
import backends.common.models.core.Item;

import java.util.HashSet;

public class User extends Account {
    HashSet<Item> items = new HashSet<>();
    private double balance;

    public double getBalance() {
        return balance;
    }
    public void setBalance(double balance){
        this.balance = balance;
    }

    public User(String id, String name, String email, String phoneNumber, String password , double balance) {
        super(id, name, email,phoneNumber, password);
        this.balance = balance;
        this.role="User";
    }
    public User(String id, String name, String email, String phoneNumber, String password) {
        super(id, name, email,phoneNumber,  password);
        this.role="User";
    }

    public User(String name, String email, String phoneNumber, String password) {
        this(buildGeneratedId(phoneNumber), name, email,phoneNumber,  password);
        this.balance = 0.0;
        this.role="User";
    }

    private static String buildGeneratedId(String phoneNumber) {
        String normalizedPhoneNumber = phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");
        if (normalizedPhoneNumber.isBlank()) {
            return "USER";
        }
        return "USER" + normalizedPhoneNumber;
    }
}
