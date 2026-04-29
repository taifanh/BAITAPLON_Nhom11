package models.bidding;

import models.Extra.IdGenerator;
import models.core.Item;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Auction {
    public enum Status {
        SCHEDULED,
        ACTIVE,
        ENDED,
        CANCELLED
    }

    private final String auctionId;
    private final Item item;
    private final List<BidTransaction> bids;
    private Status status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private double currentHighestBid;
    private BidTransaction highestBid;
    private String currentHighestBidderId;

    // Tao mot auction moi cho item vua duoc dua vao phien.
    public Auction(Item item) {
        this.auctionId = generateAuctionId();
        this.item = item;
        this.bids = new ArrayList<>();
        this.status = Status.SCHEDULED;
        this.currentHighestBid = 0;
    }

    // Constructor private de khoi phuc auction da ton tai tu DB.
    private Auction(
            String auctionId,
            Item item,
            Status status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            double currentHighestBid,
            String currentHighestBidderId
    ) {
        this.auctionId = auctionId;
        this.item = item;
        this.bids = new ArrayList<>();
        this.status = status;
        this.startAt = startAt;
        this.endAt = endAt;
        this.currentHighestBid = currentHighestBid;
        this.currentHighestBidderId = currentHighestBidderId;
    }

    // Factory de hydrate Auction tu du lieu DB luc khoi dong lai app/server.
    public static Auction restore(
            String auctionId,
            Item item,
            Status status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            double currentHighestBid,
            String currentHighestBidderId
    ) {
        return new Auction(auctionId, item, status, startAt, endAt, currentHighestBid, currentHighestBidderId);
    }

    // Tao id moi cho auction moi.
    private String generateAuctionId() {
        return "AUC" + models.core.Entity.makeItemId(IdGenerator.nextId());
    }

    // Them mot bid moi vao phien, dong thoi cap nhat gia cao nhat hien tai trong RAM.
    public synchronized void addBid(BidTransaction bid) {
        closeIfExpired();
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("Auction is not active");
        }
        if (bid == null) {
            throw new IllegalArgumentException("Bid cannot be null");
        }
        if (bid.getAmount() <= currentHighestBid) {
            throw new IllegalArgumentException("Bid amount must be higher than current highest bid");
        }

        bids.add(bid);
        currentHighestBid = bid.getAmount();
        highestBid = bid;
        currentHighestBidderId = bid.getBidderId();
    }

    // Dat lich bat dau va thoi diem ket thuc cho phien truoc khi kich hoat.
    public void schedule(LocalDateTime startAt, Duration duration) {
        if (startAt == null || duration == null) {
            throw new IllegalArgumentException("Start time and duration are required");
        }
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Auction duration must be greater than zero");
        }

        this.startAt = startAt;
        this.endAt = startAt.plus(duration);
        this.status = Status.SCHEDULED;
    }

    // Chuyen phien sang ACTIVE.
    public void start(LocalDateTime time) {
        if (time == null) {
            throw new IllegalArgumentException("Start time is required");
        }
        this.startAt = time;
        if (endAt != null && !endAt.isAfter(time) && !endAt.equals(time)) {
            throw new IllegalStateException("End time must be after or equal to start time");
        }
        this.status = Status.ACTIVE;
    }

    // Dong phien tai mot thoi diem cu the.
    public synchronized void end(LocalDateTime time) {
        if (status == Status.ENDED) {
            return;
        }
        if (status == Status.CANCELLED) {
            throw new IllegalStateException("Auction has been cancelled");
        }
        if (time == null) {
            throw new IllegalArgumentException("End time is required");
        }
        if (startAt != null && !time.isAfter(startAt)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.endAt = time;
        this.status = Status.ENDED;
    }

    // Huy phien dau gia va gan moc thoi gian dong neu can.
    public synchronized void cancel() {
        if (status == Status.ENDED) {
            throw new IllegalStateException("Auction has already ended");
        }
        this.status = Status.CANCELLED;
        if (endAt == null) {
            this.endAt = LocalDateTime.now();
        }
    }

    // Kiem tra xem phien co dang ACTIVE hay khong.
    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    // Tra ve id cua phien dau gia.
    public String getAuctionId() {
        return auctionId;
    }

    // Tra ve trang thai hien tai cua phien.
    public Status getStatus() {
        return status;
    }

    // Tra ve thoi diem bat dau.
    public LocalDateTime getStartAt() {
        return startAt;
    }

    // Tra ve thoi diem ket thuc.
    public LocalDateTime getEndAt() {
        return endAt;
    }

    // Tra ve muc gia cao nhat hien tai.
    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    // Tra ve bid cao nhat trong RAM, neu bid do duoc tao trong session hien tai.
    public BidTransaction getHighestBid() {
        return highestBid;
    }

    // Tra ve id nguoi dang giu gia cao nhat, duoc dung de persist winner qua cac lan restart.
    public String getCurrentHighestBidderId() {
        return currentHighestBidderId;
    }

    // Tra ve item dang duoc dau gia.
    public Item getItem() {
        return item;
    }

    // Tra ve danh sach bid trong RAM, chi phan anh cac bid da duoc nap vao object hien tai.
    public List<BidTransaction> getBidList() {
        return Collections.unmodifiableList(bids);
    }

    // Neu phien da qua han thi dong ngay lap tuc. Method nay giup chan bid vao phien da het gio.
    public synchronized boolean closeIfExpired() {
        if (status != Status.ACTIVE || endAt == null) {
            return false;
        }
        if (LocalDateTime.now().isBefore(endAt)) {
            return false;
        }
        end(LocalDateTime.now());
        return true;
    }
}
