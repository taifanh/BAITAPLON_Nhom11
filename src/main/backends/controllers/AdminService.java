package controllers;

import com.google.gson.Gson;
import models.accounts.Admin;
import models.bidding.Auction;
import models.core.Item;

import java.io.IOException;

public final class AdminService {
    private static final String ADD_ITEM_REQUEST_TYPE = "additem";
    private static final Gson GSON = new Gson();

    private AdminService() {
    }

    public static Auction startAuction(Admin admin, int hours, int minutes, int seconds) throws IOException {
        return AuctionService.startAuction(admin, hours, minutes, seconds);
    }

    public static Auction startAuction(Admin admin, Item item, int hours, int minutes, int seconds) throws IOException {
        return AuctionService.startAuction(admin, item, hours, minutes, seconds);
    }
}
