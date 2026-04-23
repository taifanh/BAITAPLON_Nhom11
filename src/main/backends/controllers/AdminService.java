package controllers;

import models.accounts.Admin;
import models.bidding.Auction;

import java.io.IOException;

public final class AdminService {
    private AdminService() {
    }

    public static Auction startAuction(Admin admin, int hours, int minutes, int seconds) throws IOException {
        return AuctionService.startAuction(admin, hours, minutes, seconds);
    }
}
