package controllers;

import Database.Auctions;
import Database.BidTransactions;
import Database.Inventory;
import controllers.Server.ServerAuctionManager;
import models.accounts.Admin;
import models.accounts.User;
import models.bidding.Auction;
import models.bidding.BidTransaction;
import models.core.Item;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class AuctionService {
    private static final ScheduledExecutorService AUCTION_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "auction-auto-close");
                thread.setDaemon(true);
                return thread;
            });
    private static final Map<String, Auction> ACTIVE_AUCTIONS = new ConcurrentHashMap<>();
    private static final Map<String, ScheduledFuture<?>> AUTO_CLOSE_TASKS = new ConcurrentHashMap<>();
    private static volatile boolean restoredOnStartup = false;

    private AuctionService() {
    }

    // Tao mot phien moi, luu vao DB, dua item vao trang thai dang dau gia
    // va dang ky job tu dong dong phien khi het gio.
    public static Auction startAuction(Admin admin, int hours, int minutes, int seconds) throws IOException {
        if (admin == null) {
            throw new SecurityException("Chi admin moi duoc phep bat dau phien dau gia");
        }

        Duration duration = Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Thoi gian dau gia phai lon hon 0");
        }

        Inventory inventory = new Inventory();
        Item itemAuction = inventory.getItemtoAuction(Inventory.STATUS_IN_PROGRESS);
        if (itemAuction == null) {
            throw new IllegalStateException("Khong co san pham nao o trang thai STATUS_IN_PROGRESS");
        }

        Auction auction = new Auction(itemAuction);
        LocalDateTime now = LocalDateTime.now();
        auction.schedule(now, duration);
        auction.start(now);

//        Auctions auctionsRepository = new Auctions();
//        auctionsRepository.saveAuction(auction);
//        inventory.updateItemStatus(itemAuction.getId(), Inventory.STATUS_IN_PROGRESS);
        registerActiveAuction(auction);
        scheduleAutoClose(auction, duration);
        return auction;
    }

    // Tao phien dau gia cho item duoc chi dinh
    public static Auction startAuction(Admin admin, Item item, int hours, int minutes, int seconds) throws IOException {
        if (admin == null) {
            throw new SecurityException("Chi admin moi duoc phep bat dau phien dau gia");
        }
        
        if (item == null) {
            throw new IllegalArgumentException("San pham khong hop le");
        }

        Duration duration = Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Thoi gian dau gia phai lon hon 0");
        }
        Inventory inventory = new Inventory();
        inventory.updateItemStatus(item.getId(), Inventory.STATUS_IN_PROGRESS);

        Auction auction = new Auction(item);
        LocalDateTime now = LocalDateTime.now();
        auction.schedule(now, duration);
        registerActiveAuction(auction);

        try {
            auction.start(now);
            scheduleAutoClose(auction, duration);
        } catch (Exception e) {
            unregisterActiveAuction(item.getId());
            throw e;
        }
        return auction;
    }

    // Dat gia cho mot phien dang active. Method nay dung instance Auction
    // duoc service quan ly neu da co trong registry, de tranh su dung object stale sau khi restore.
    public static BidTransaction placeBid(User user, Auction auction, double amount) throws IOException {
        if (user == null) {
            throw new IllegalArgumentException("Nguoi dung dau gia khong hop le");
        }
        if (auction == null) {
            throw new IllegalArgumentException("Phien dau gia khong hop le");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Gia bid phai lon hon 0");
        }

        Auction managedAuction = resolveAuction(auction);
        Inventory inventory = new Inventory();
        String sellerId = inventory.getUserIdByItemId(managedAuction.getItem().getId());
        if (user.getId().equals(sellerId)) {
            throw new IllegalArgumentException("Nguoi ban khong duoc dau gia san pham minh ban");
        }

        BidTransaction bid = new BidTransaction(user, managedAuction.getItem(), amount);
        managedAuction.addBid(bid);

        Auctions auctionsRepository = new Auctions();
        auctionsRepository.updateHighestBid(
                managedAuction.getAuctionId(),
                managedAuction.getCurrentHighestBid(),
                managedAuction.getCurrentHighestBidderId()
        );

        BidTransactions bidTransactions = new BidTransactions();
        bidTransactions.saveBid(managedAuction.getAuctionId(), bid);
        return bid;
    }

    // Dong phien va dong bo ket qua ve DB. Dong thoi bo phien nay khoi registry active
    // va huy job auto-close neu no con ton tai.
    public static void endAuction(Auction auction, LocalDateTime time) throws IOException {
        if (auction == null) {
            throw new IllegalArgumentException("Phien dau gia khong hop le");
        }

        Auction managedAuction = resolveAuction(auction);
        managedAuction.end(time);
        syncAuctionClosure(managedAuction);
        unregisterActiveAuction(managedAuction.getItem().getId());
    }

    // Huy phien dang dau gia, dua item ve lai WAITING va bo khoi registry active.
    public static void cancelAuction(Auction auction) throws IOException {
        if (auction == null) {
            throw new IllegalArgumentException("Phien dau gia khong hop le");
        }

        Auction managedAuction = resolveAuction(auction);
        managedAuction.cancel();
        syncAuctionCancellation(managedAuction);
        unregisterActiveAuction(managedAuction.getItem().getId());
    }

    // Khoi phuc cac phien ACTIVE tu DB khi app/server bat lai.
    // Phien qua han se bi dong ngay, phien con han se duoc schedule lai voi thoi gian con lai.
    public static synchronized void restoreActiveAuctionsOnStartup() throws IOException {
        if (restoredOnStartup) {
            return;
        }

        Auctions auctions = new Auctions();
        List<Auction> activeAuctions = auctions.getActiveAuctions();
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : activeAuctions) {
            if (auction.getEndAt() == null || !auction.getEndAt().isAfter(now)) {
                endAuction(auction, now);
                continue;
            }

            registerActiveAuction(auction);
            Duration remaining = Duration.between(now, auction.getEndAt());
            scheduleAutoClose(auction, remaining);
        }

        restoredOnStartup = true;
    }

    // Tra ve mot snapshot danh sach phien active dang duoc service quan ly trong RAM.
    public static List<Auction> getManagedActiveAuctions() {
        return List.copyOf(ACTIVE_AUCTIONS.values());
    }

    // Tim phien active theo id tu registry trong RAM.
    public static Auction getManagedActiveAuction(String itemId) {
        return ACTIVE_AUCTIONS.get(itemId);
    }

    // Dang ky job dong phien sau mot khoang thoi gian con lai.
    private static void scheduleAutoClose(Auction auction, Duration duration) {
        cancelScheduledTask(auction.getAuctionId());
        ScheduledFuture<?> future = AUCTION_SCHEDULER.schedule(() -> {
            try {
                endAuction(auction, LocalDateTime.now());
                ServerAuctionManager.getInstance().broadcastEnd(auction.getItem().getId());
            } catch (Exception e) {
                System.err.println("Khong the tu dong dong phien dau gia " + auction.getAuctionId() + ": " + e.getMessage());
            }
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
        AUTO_CLOSE_TASKS.put(auction.getAuctionId(), future);
    }

    // Cap nhat trang thai dong phien, winner va trang thai item sau khi phien ket thuc.
    private static void syncAuctionClosure(Auction auction) throws IOException {
        Auctions auctions = new Auctions();
        Inventory inventory = new Inventory();
        auctions.updateAuctionState(
                auction.getAuctionId(),
                auction.getStatus(),
                auction.getEndAt(),
                auction.getCurrentHighestBid(),
                auction.getCurrentHighestBidderId()
        );

        String itemStatus = auction.getCurrentHighestBidderId() == null
                ? Inventory.STATUS_UNSOLD
                : Inventory.STATUS_SOLD;
        inventory.updateItemStatus(auction.getItem().getId(), itemStatus);
    }

    // Cap nhat DB khi huy phien va dua item tro lai hang doi WAITING.
    private static void syncAuctionCancellation(Auction auction) throws IOException {
        Auctions auctions = new Auctions();
        Inventory inventory = new Inventory();
        auctions.updateAuctionState(
                auction.getAuctionId(),
                auction.getStatus(),
                auction.getEndAt(),
                auction.getCurrentHighestBid(),
                null
        );
        inventory.updateItemStatus(auction.getItem().getId(), Inventory.STATUS_WAITING);
    }

    // Lay instance Auction dang duoc service quan ly neu da ton tai,
    // nguoc lai dung object duoc truyen vao va dang ky no vao registry neu no dang ACTIVE.
    private static Auction resolveAuction(Auction auction) {
        Auction managedAuction = ACTIVE_AUCTIONS.get(auction.getItem().getId());
        if (managedAuction != null) {
            return managedAuction;
        }
        if (auction.isActive()) {
            registerActiveAuction(auction);
        }
        return auction;
    }

    // Dua phien vao registry de cac tac vu tiep theo luon su dung cung mot instance trong RAM.
    private static void registerActiveAuction(Auction auction) {
        ACTIVE_AUCTIONS.put(auction.getItem().getId(), auction);
    }

    // Xoa phien khoi registry active va huy job auto-close da dang ky truoc do.
    private static void unregisterActiveAuction(String itemId) {
        ACTIVE_AUCTIONS.remove(itemId);
        cancelScheduledTask(itemId);
    }

    public static Duration getDuration(String itemId) {
        Auction aut = ACTIVE_AUCTIONS.get(itemId);
        if (aut == null) {
            return Duration.ZERO;
        }
        return Duration.between(LocalDateTime.now(), aut.getEndAt());
    }

    // Huy job scheduler cu neu dang ton tai, tranh bi lap lich 2 lan cho cung mot phien.
    private static void cancelScheduledTask(String auctionId) {
        ScheduledFuture<?> future = AUTO_CLOSE_TASKS.remove(auctionId);
        if (future != null) {
            future.cancel(false);
        }
    }
}
