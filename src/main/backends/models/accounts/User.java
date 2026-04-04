package models.accounts;

import models.core.Entity;

public class User extends Entity {
    protected String name;
    protected String phoneNumber;
    protected String email;
    protected String password;


    public User(String id, String name, String phoneNumber, String email, String password) {
        this.id = id;// id nhận danh khi extends entity
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.password = password;
    }

    // khởi tạo đối tượng khi đăng kí ( vì ID sẽ do hệ thống tự tạo)
    public User(String name, String phoneNumber, String email, String password){
        super(phoneNumber);
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.password = password;
    }
   // cần 2 constructor khác nhau để đảm bảo id không thay đôir khi thay đổi phone number vì id được khởi tạo lúc đầu phụ thuộc phone number

}
