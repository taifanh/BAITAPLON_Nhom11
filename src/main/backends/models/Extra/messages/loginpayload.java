package models.Extra.messages;

public class loginpayload {
    private String role;

    public loginpayload(String role){
        this.role = role;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {}

    public loginpayload(){}
}
