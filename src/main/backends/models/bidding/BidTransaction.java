package models.bidding;

import models.accounts.User;
import models.core.Account;
import models.core.Item;

import java.util.Date;

public class BidTransaction {
    private final User bidder;
    private final Item item;
    private final double amount;
    private final Date time;

    public BidTransaction(User bidder, Item item, double amount) {
        this.bidder = bidder;
        this.item = item;
        this.amount = amount;
        this.time = new Date();
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
