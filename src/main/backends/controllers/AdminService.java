package controllers;

import Database.Auctions;
import Database.Inventory;
import models.accounts.Admin;
import models.bidding.Auction;
import models.core.Item;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

public final class AdminService {
    private AdminService() {
    }

    public static Auction startAuction(Admin admin, int hours, int minutes, int seconds) throws IOException {
        //Chan ngay tu dau neu khong co doi tuong admin goi vao service.
        if (admin == null) {
            throw new SecurityException("Chi admin moi duoc phep bat dau phien dau gia");
        }

        Duration duration = Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Thoi gian dau gia phai lon hon 0");
        }

        Inventory inventory = new Inventory();
        Item waitingItem = inventory.getItemByStatus(Inventory.STATUS_WAITING);
        if (waitingItem==null) {
            throw new IllegalStateException("Khong co san pham nao o trang thai WAITING");
        }

        // Tao auction va dua toan bo item WAITING vao phien dau gia.
        Auction auction = new Auction(waitingItem);

        // Set thoi gian bat dau va thoi gian ket thuc cho auction.
        LocalDateTime now = LocalDateTime.now();
        auction.schedule(now, duration);
        auction.start(now);

        // Luu auction vao bang auctions.
        Auctions auctionsRepository = new Auctions();
        auctionsRepository.saveAuction(auction);

        String ItemId=waitingItem.getId();

        inventory.updateItemStatus(ItemId, Inventory.STATUS_IN_AUCTION);
        return auction;
    }

}
