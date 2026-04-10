package models.accounts;


import controllers.UserStore;
import models.core.Account;

import java.io.IOException;
import java.util.Random;


public class User extends Account {
    public User() {}

   // constrcuctor cho khi đăng nhập
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
        this.id = generateEntity();
    }
    public String generateEntity() {
        Random RANDOM = new Random();
        UserStore userStore = new UserStore();
        String generatedId;

        do {
            generatedId = "USER" + (100000 + RANDOM.nextInt(899999));
        } while (isExistingId(userStore, generatedId));

        return generatedId;
    }

    private boolean isExistingId(UserStore userStore, String id) {
        try {
            return userStore.IDexist(id);
        } catch (IOException e) {
            throw new RuntimeException("Khong the kiem tra ID da ton tai hay chua.", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

   // cần 2 constructor khác nhau để đảm bảo id không thay đôir khi thay đổi phone number vì id được khởi tạo lúc đầu phụ thuộc phone number
}
