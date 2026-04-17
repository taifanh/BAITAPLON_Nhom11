package models.accounts;

import controllers.AdminService;
import javafx.scene.control.Alert;
import models.bidding.Auction;
import models.core.Account;

import java.io.IOException;

public class Admin extends Account {
    private String name;
    private String phoneNumber;
    private String email;
    private String password;

    private static Admin instance;// có 1 admin duy nhất

    // constructor  khi load từ file (đăng nhập )
    public Admin(String id, String name, String phoneNumber, String email, String password) {
        this.id = id;// id nhận danh khi extends entity
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.password = password;
    }

    // khởi tạo đối tượng khi đăng kí ( vì ID sẽ do hệ thống tự tạo)
    private Admin(String name, String phoneNumber, String email, String password){
       super(name , phoneNumber , email , password);
    }


    // hàm tạo admin chỉ cho tồn tại 1 admin
    public void creating_admin(String name, String phoneNumber,String email, String password){
        if ( instance == null){
            instance = new Admin(name,phoneNumber,email,password);
        }
        else// lỗi khi cố đăng kí 2 admin
        {Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("admin");
            alert.setHeaderText("admin register error");
            alert.setContentText("one admin existed");
            alert.showAndWait();}
    }

    public Auction startAuction(int hours, int minutes, int seconds) {
        try {
            // Service nhan vao doi tuong admin de goi StartAuction
            return AdminService.startAuction(this, hours, minutes, seconds);
        } catch (IOException e) {
            throw new RuntimeException("Khong the bat dau phien dau gia", e);
        }
    }

    public Auction StartAuction(int hours, int minutes, int seconds) {
        return startAuction(hours, minutes, seconds);
    }

    public void manageAuction(Auction auction){}

    public void createsession(String s){

    }
}
