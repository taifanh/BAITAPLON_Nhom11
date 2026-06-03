package backends.server.service;

import backends.common.messages.MsgAuction.AdminActionCommand;
import backends.server.database.*;
import backends.server.handler.ServerAuctionManager;
import backends.common.messages.MsgBid.ServerBidRespond;
import backends.common.models.accounts.Admin;
import backends.common.models.bidding.Auction;
import backends.common.models.core.Item;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.sql.SQLException;
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

    private static final long SNIPE_WINDOW_SECONDS = 5;
    private static final long SNIPE_EXTENSION_SECONDS = 10;
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

        InventoryDAO inventoryDAO = new InventoryDAO();
        Item itemAuction = inventoryDAO.getItemtoAuction(InventoryDAO.STATUS_IN_PROGRESS);
        if (itemAuction == null) {
            throw new IllegalStateException("Khong co san pham nao o trang thai STATUS_IN_PROGRESS");
        }

        Auction auction = new Auction(itemAuction);
        LocalDateTime now = LocalDateTime.now();
        auction.schedule(now, duration);
        auction.start(now);

        AuctionDAO auctionDAO = new AuctionDAO();
        auctionDAO.saveAuction(auction);
        registerActiveAuction(auction);
        scheduleAutoClose(auction, duration);
        
        // Cập nhật request status khi bắt đầu đấu giá
        MyRequestDAO myRequestDAO = new MyRequestDAO();
        String requestId = inventoryDAO.getRequestIdbyItem(itemAuction.getId());
        if (requestId != null) {
            // Start auction = đổi trạng thái request/item + phát event cho seller-side list.
            myRequestDAO.updateRequestStatus(requestId, MyRequestDAO.STATUS_IN_PROGRESS);
            RequestStatusNotifier.notifyByRequestId(requestId, itemAuction.getId(), MyRequestDAO.STATUS_IN_PROGRESS);
        }


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
        InventoryDAO inventoryDAO = new InventoryDAO();
        inventoryDAO.updateItemStatus(item.getId(), InventoryDAO.STATUS_IN_PROGRESS);
        MyRequestDAO myRequestDAO = new MyRequestDAO();
        String requestId = inventoryDAO.getRequestIdbyItem(item.getId());
        if (requestId != null) {
            // Cùng một luồng status cho cả nhánh start từ admin và nhánh start theo item chỉ định.
            myRequestDAO.updateRequestStatus(requestId, MyRequestDAO.STATUS_IN_PROGRESS);
            RequestStatusNotifier.notifyByRequestId(requestId, item.getId(), MyRequestDAO.STATUS_IN_PROGRESS);
        }

        Auction auction = new Auction(item);
        LocalDateTime now = LocalDateTime.now();
        auction.schedule(now, duration);

        try {
            auction.start(now);
            AuctionDAO auctionDAO = new AuctionDAO();
            auctionDAO.saveAuction(auction);
            registerActiveAuction(auction);
            scheduleAutoClose(auction, duration);
        } catch (Exception e) {
            unregisterActiveAuction(auction);
            throw e;
        }
        return auction;
    }

    // Dong phien va dong bo ket qua ve DB. Dong thoi bo phien nay khoi registry active
    // va huy job auto-close neu no con ton tai.
    public static void endAuction(Auction auction, LocalDateTime time) throws IOException {
        if (auction == null) {
            throw new IllegalArgumentException("Phien dau gia khong hop le");
        }

        Auction managedAuction = resolveAuction(auction);
        managedAuction.end(time);
        settleAuctionBalance(managedAuction);
        syncWinnerFromPersistedBids(managedAuction);
        syncAuctionClosure(managedAuction);
        unregisterActiveAuction(managedAuction);
    }


    // Khoi phuc cac phien ACTIVE tu DB khi app/server bat lai.
    // Phien qua han se bi dong ngay, phien con han se duoc schedule lai voi thoi gian con lai.
    public static synchronized void restoreActiveAuctionsOnStartup() throws IOException {
        if (restoredOnStartup) {
            return;
        }

        AuctionDAO auctionDAO = new AuctionDAO();
        List<Auction> activeAuctions = auctionDAO.getActiveAuctions();
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

    private static void settleAuctionBalance(Auction auction) throws IOException {
        BidTransactionDAO bidDAO = new BidTransactionDAO();
        ServerBidRespond maxBidder;
        try {
            maxBidder = bidDAO.getMaxBidder(auction.getAuctionId());
        } catch (SQLException e) {
            throw new IOException("Khong the doc winner", e);
        }

        if (maxBidder == null || maxBidder.userId == null) return;

        InventoryDAO inventoryDAO = new InventoryDAO();
        UserDAO userDAO = new UserDAO();
        String sellerId = inventoryDAO.getUserIdByItemId(auction.getItem().getId());

        userDAO.updateBalance(-maxBidder.amount, maxBidder.userId);
        userDAO.updateBalance( maxBidder.amount, sellerId);
    }

    // service sẽ tăng thêm thời gian
    public static boolean extendAuctionIfNeeded(String itemId) throws IOException {
        Auction auction = ACTIVE_AUCTIONS.get(itemId);
        if (auction == null || auction.getEndAt() == null || !auction.isActive()) {
            return false;
        }// check whehter auction is still available

        Duration remaining = Duration.between(LocalDateTime.now(), auction.getEndAt());
        if (remaining.isNegative() || remaining.isZero()) {
            return false;
        }// check whether auction is ended or not

        if (remaining.getSeconds() < SNIPE_WINDOW_SECONDS) {// check whether time coutndown is within 5 seconds
            auction.extendEndAt(Duration.ofSeconds(SNIPE_EXTENSION_SECONDS));

            AuctionDAO auctions = new AuctionDAO();
            auctions.updateEndTime(auction.getAuctionId(), auction.getEndAt());

            Duration newRemaining = Duration.between(LocalDateTime.now(), auction.getEndAt());
            scheduleAutoClose(auction, newRemaining);
            return true;// báo lại server là cần tăng đếm giờ
        }

        return false;
    }



    public static Auction getManagedActiveAuctionByAuctionId(String auctionId) {
        for (Auction auction : ACTIVE_AUCTIONS.values()) {
            if (auction.getAuctionId().equals(auctionId)) {
                return auction;
            }
        }
        return null;
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
                ServerAuctionManager.getInstance().broadcastEnd(auction.getItem().getId(), auction);
            } catch (Exception e) {
                System.err.println("Khong the tu dong dong phien dau gia " + auction.getAuctionId() + ": " + e.getMessage());
            }
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
        AUTO_CLOSE_TASKS.put(auction.getAuctionId(), future);
    }

    // Cap nhat trang thai dong phien, winner va trang thai item sau khi phien ket thuc.
    private static void syncAuctionClosure(Auction auction) throws IOException {
        AuctionDAO auctionDAO = new AuctionDAO();
        InventoryDAO inventoryDAO = new InventoryDAO();
        MyRequestDAO requestDAO = new  MyRequestDAO();

        auctionDAO.updateAuctionState(
                auction.getAuctionId(),
                auction.getStatus(),
                auction.getEndAt(),
                auction.getCurrentHighestBid(),
                auction.getCurrentHighestBidderId()
        );

        String itemStatus = auction.getCurrentHighestBidderId() == null
                ? InventoryDAO.STATUS_UNSOLD
                : InventoryDAO.STATUS_SOLD;
        inventoryDAO.updateItemStatus(auction.getItem().getId(), itemStatus);
        String requestId = inventoryDAO.getRequestIdbyItem(auction.getItem().getId());
        // Kết phiên cũng dùng cùng cơ chế: update DB trước, notify sau để UI không phải reload toàn list.
        requestDAO.updateRequestStatus(requestId, itemStatus);
        RequestStatusNotifier.notifyByRequestId(requestId, auction.getItem().getId(), itemStatus);
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
    private static void unregisterActiveAuction(Auction auction) {
        ACTIVE_AUCTIONS.remove(auction.getItem().getId());
        cancelScheduledTask(auction.getAuctionId());
    }

    public static Duration getDuration(String itemId) {
        Auction aut = ACTIVE_AUCTIONS.get(itemId);
        if (aut == null || aut.getEndAt() == null) {
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

    private static void syncWinnerFromPersistedBids(Auction auction) throws IOException {
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
        ServerBidRespond maxBidder;
        try {
            maxBidder = bidTransactionDAO.getMaxBidder(auction.getAuctionId());
        } catch (SQLException e) {
            throw new IOException("Khong the doc winner cua auction " + auction.getAuctionId(), e);
        }
        if (maxBidder == null || maxBidder.userId == null) {
            auction.syncHighestBidState(0, null);
            return;
        }

        auction.syncHighestBidState(maxBidder.amount, maxBidder.userId);
    }

}
