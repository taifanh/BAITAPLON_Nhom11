package models.accounts;

import controllers.UserJsonStore;
import models.core.Account;

import java.io.IOException;
import java.util.Random;
import models.core.Account;
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
        UserJsonStore userJsonStore = new UserJsonStore();
        String generatedId;

        do {
            generatedId = "USER" + (100000 + RANDOM.nextInt(899999));
        } while (isExistingId(userJsonStore, generatedId));

        return generatedId;
    }

    private boolean isExistingId(UserJsonStore userJsonStore, String id) {
        try {
            return userJsonStore.idExists(id);
        } catch (IOException e) {
            throw new RuntimeException("Khong the kiem tra ID da ton tai hay chua.", e);
        }
    }

   // cần 2 constructor khác nhau để đảm bảo id không thay đôir khi thay đổi phone number vì id được khởi tạo lúc đầu phụ thuộc phone number
}
