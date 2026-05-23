package backends.common.messages.Common;

public class SigninPayload {
    private String phoneNumber;
    private String password;

    public SigninPayload() {}

    public SigninPayload(String phoneNumber, String password) {
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPassword(String password) {
        this.password = password;
    }
// getter/setter
}
