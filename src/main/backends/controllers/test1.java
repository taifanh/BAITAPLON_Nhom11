package controllers;

import models.accounts.Admin;
import models.accounts.User;
import models.bidding.Auction;


import static models.items.ItemType.*;

public class test1 {
    public static void main(String[] args) {
        //Khởi tạo 2 user
        User user1 = new User("User1","12345","1","1");
        User user2 = new User("User2","34567","2","2");
        //Thêm vào inventory 3 sản phẩm
        user1.addItem(Electronics,"Laptop",5000,"new laptop");
        user1.addItem(Art,"Mona Lisa",900000,"new art");
        user2.addItem(Electronics,"Phone",3000,"new phone");

        // Tao 1 admin tam thoi de test, chua can qua DB.
        Admin admin = new Admin("ADMIN001", "Admin", "0123456789", "admin@gmail.com", "123456");

        try {
            //Mở phiên đấu giá với sản phẩm đầu tiên người bán là user 1
            //Auction auction=admin.startAuction(0, 0, 5);
            //user1.bids(auction,1500);//user 1 đấu giá sẽ báo lỗi không được đấu giá sản phẩm mình bán
            //user2.bids(auction,2000);//user 2 đấu giá sẽ cập nhập giá
            Thread.sleep(6000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
