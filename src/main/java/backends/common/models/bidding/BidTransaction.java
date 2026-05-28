package backends.common.models.bidding;

import backends.common.models.accounts.User;
import backends.common.models.core.Account;
import backends.common.models.core.Item;

import java.util.Date;

public class BidTransaction {
    private final User bidder;
    private final Item item;
    private final double amount;
    private final Date time;
    private boolean isAuto;
    private double maxBid;

    public BidTransaction(User bidder, Item item, double amount) {
        this.bidder = bidder;
        this.item = item;
        this.amount = amount;
        this.time = new Date();
        this.isAuto = false;
        this.maxBid = 0;
    }

    public boolean isAuto() { return isAuto; }
    public double getMaxBid() { return maxBid; }

    public void setAuto(boolean isAuto) { this.isAuto = isAuto; }
    public void setMaxBid(double maxBid) { this.maxBid = maxBid; }

    public void disableAuto() {
        this.isAuto = false;
        this.maxBid = 0;
    }

    public double getAmount() {
        return amount;
    }

    public User getBidder() {
        return bidder;
    }

    public String getBidderId() {
        if (bidder instanceof Account account) {
            return account.getId();
        }
        throw new IllegalStateException("Bidder does not expose an id");
    }

    public Item item() {
        return item;
    }

    public Date getTime() {
        return time;
    }
}
