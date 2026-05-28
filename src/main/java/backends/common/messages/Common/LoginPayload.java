package backends.common.messages.Common;

public class LoginPayload {
    private String role;

    public LoginPayload(String role){
        this.role = role;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public LoginPayload(){}
}
