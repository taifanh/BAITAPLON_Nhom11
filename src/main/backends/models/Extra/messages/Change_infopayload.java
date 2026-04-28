package models.Extra.messages;

public class Change_infopayload {
    private String new_name;
    private String new_email;
    private String new_phonenumber;
    private String new_password;

    public Change_infopayload(String new_name, String new_email, String new_phonenumber, String new_password) {
        this.new_name = new_name;
        this.new_email = new_email;
        this.new_phonenumber = new_phonenumber;
        this.new_password = new_password;
    }
}
