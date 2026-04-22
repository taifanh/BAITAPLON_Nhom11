package controllers;

import Database.Auctions;
import Database.Inventory;
import models.accounts.Admin;
import models.bidding.Auction;
import models.core.Item;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AdminService {

    private static final ScheduledExecutorService AUCTION_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "auction-auto-close");
                thread.setDaemon(true);
                return thread;
            });

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
        Item waitingItem = inventory.getItemtoAuction(Inventory.STATUS_WAITING);
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
        scheduleAutoClose(auction, duration);

        return auction;
    }

    private static void scheduleAutoClose(Auction auction, Duration duration) {
        AUCTION_SCHEDULER.schedule(() -> {
            try {
                auction.end(LocalDateTime.now());
            } catch (Exception e) {
                System.err.println("Khong the tu dong dong phien dau gia " + auction.getAuctionId() + ": " + e.getMessage());
            }
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
    }
}
