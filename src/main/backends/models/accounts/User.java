package models.accounts;

import models.core.Account;

public class User extends Account {
    public User() {}


    public User(String id, String name, String phoneNumber, String email, String password) {
        this.id = id;// id nhận danh khi extends entity
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.password = password;
    }

    // khởi tạo đối tượng khi đăng kí ( vì ID sẽ do hệ thống tự tạo)
    public User(String name, String phoneNumber, String email, String password){
        super(name , phoneNumber , email , password);
    }
   // cần 2 constructor khác nhau để đảm bảo id không thay đôir khi thay đổi phone number vì id được khởi tạo lúc đầu phụ thuộc phone number

}
