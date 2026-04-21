package models.bidding;

import models.Extra.IdGenerator;
import models.core.Item;

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

    public void addBid(BidTransaction bid) {
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

    public void end(LocalDateTime time) {
        if (time == null) {
            throw new IllegalArgumentException("End time is required");
        }
        if (startAt != null && !time.isAfter(startAt)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.endAt = time;
        this.status = Status.ENDED;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
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
}
