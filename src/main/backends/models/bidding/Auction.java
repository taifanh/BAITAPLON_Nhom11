package models.bidding;

import Database.Auctions;
import Database.Inventory;
import models.Extra.IdGenerator;
import models.core.Item;

import java.io.IOException;
import java.util.ArrayList;
import java.time.Duration;
import java.time.LocalDateTime;
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

    public Auction(Item item) {
        this.auctionId = generateAuctionId();
        this.item = item;
        this.bids = new ArrayList<>();
        this.status = Status.SCHEDULED;
        this.currentHighestBid = 0;
    }

    private String generateAuctionId() {
        return "AUC" + models.core.Entity.makeItemId(IdGenerator.nextId());
    }

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
        persistHighestBid();
    }

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

    public void start(LocalDateTime time) {
        if (time == null) {
            throw new IllegalArgumentException("Start time is required");
        }
        this.startAt = time;
        if (endAt != null && !endAt.isAfter(time)) {
            throw new IllegalStateException("End time must be after start time");
        }
        this.status = Status.ACTIVE;
    }

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
        syncClosedAuction();
    }

    public synchronized void cancel() {
        this.status = Status.CANCELLED;
        syncCancelledAuction();
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public BidTransaction getHighestBid() {
        return highestBid;
    }

    public Item getItem() {
        return item;
    }

    public List<BidTransaction> getBidList() {
        return Collections.unmodifiableList(bids);
    }

    //đóng phiên khi hết thời gian
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

    private void persistHighestBid() {
        try {
            Auctions auctions = new Auctions();
            auctions.updateHighestBid(auctionId, currentHighestBid);
        } catch (IOException e) {
            throw new RuntimeException("Khong the cap nhat highest bid", e);
        }
    }

    private void syncClosedAuction() {
        try {
            Auctions auctions = new Auctions();
            Inventory inventory = new Inventory();
            auctions.updateAuctionState(auctionId, status, endAt, currentHighestBid);
            String itemStatus = highestBid == null ? Inventory.STATUS_UNSOLD : Inventory.STATUS_SOLD;
            inventory.updateItemStatus(item.getId(), itemStatus);
        } catch (IOException e) {
            throw new RuntimeException("Khong the dong phien dau gia", e);
        }
    }
    //hủy phiên đấu giá
    private void syncCancelledAuction() {
        try {
            Auctions auctions = new Auctions();
            Inventory inventory = new Inventory();
            LocalDateTime closedAt = endAt != null ? endAt : LocalDateTime.now();
            this.endAt = closedAt;
            auctions.updateAuctionState(auctionId, status, closedAt, currentHighestBid);
            inventory.updateItemStatus(item.getId(), Inventory.STATUS_WAITING);
        } catch (IOException e) {
            throw new RuntimeException("Khong the huy phien dau gia", e);
        }
    }
}
