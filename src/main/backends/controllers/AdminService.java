package controllers;

import Database.Inventory;
import Database.request_log;
import com.google.gson.Gson;
import models.Extra.messages.Createitempayload;
import models.accounts.Admin;
import models.bidding.Auction;
import models.core.Item;
import models.items.ItemType;
import models.items.itemFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AdminService {
    private static final String ADD_ITEM_REQUEST_TYPE = "additem";
    private static final Gson GSON = new Gson();

    private AdminService() {
    }

    public static Auction startAuction(Admin admin, int hours, int minutes, int seconds) throws IOException {
        return AuctionService.startAuction(admin, hours, minutes, seconds);
    }

    public static List<PendingItemRequest> getPendingItemRequests() throws IOException {
        request_log requestRepository = new request_log();
        List<PendingItemRequest> pendingRequests = new ArrayList<>();
        for (request_log.RequestRecord record : requestRepository.getRequestsByType(ADD_ITEM_REQUEST_TYPE)) {
            PendingItemRequest parsed = toPendingItemRequest(record);
            if (parsed != null) {
                pendingRequests.add(parsed);
            }
        }
        pendingRequests.sort(Comparator.comparingInt(PendingItemRequest::requestId));
        return pendingRequests;
    }

    public static List<Item> getWaitingInventoryItems() throws IOException {
        Inventory inventory = new Inventory();
        return inventory.getItemsByStatus(Inventory.STATUS_WAITING);
    }

    public static List<Item> getItemsInAuction() throws IOException {
        Inventory inventory = new Inventory();
        return inventory.getItemsByStatus(Inventory.STATUS_IN_AUCTION);
    }

    public static void acceptRequests(List<Integer> requestIds) throws IOException {
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }

        List<PendingItemRequest> allRequests = getPendingItemRequests();
        List<PendingItemRequest> approvedRequests = allRequests.stream()
                .filter(request -> requestIds.contains(request.requestId()))
                .toList();

        Inventory inventory = new Inventory();
        for (PendingItemRequest request : approvedRequests) {
            Item item = itemFactory.createItem(
                    request.itemType(),
                    request.derivedItemName(),
                    request.basePrice(),
                    request.itemInfo()
            );
            inventory.saveItem(item, request.userId());
        }

        request_log requestRepository = new request_log();
        requestRepository.deleteRequests(requestIds);
    }

    public static void rejectRequests(List<Integer> requestIds) throws IOException {
        request_log requestRepository = new request_log();
        requestRepository.deleteRequests(requestIds);
    }

    public static void moveInventoryItemToBidding(String itemId) throws IOException {
        if (itemId == null || itemId.isBlank()) {
            return;
        }

        Inventory inventory = new Inventory();
        inventory.updateItemStatus(itemId, Inventory.STATUS_IN_AUCTION);
    }

    private static PendingItemRequest toPendingItemRequest(request_log.RequestRecord record) {
        try {
            Createitempayload payload = GSON.fromJson(record.requestInfo(), Createitempayload.class);
            if (payload == null) {
                return null;
            }

            String itemInfo = payload.getItemInfo() == null ? "" : payload.getItemInfo().trim();
            ItemType itemType = resolveItemType(payload.getItemType());
            double basePrice = payload.getBasePrice();
            double bidIncrement = payload.getBidIncrement();

            return new PendingItemRequest(
                    record.id(),
                    record.userId(),
                    itemType,
                    deriveItemName(itemType, itemInfo, record.id()),
                    itemInfo,
                    basePrice,
                    bidIncrement
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemType resolveItemType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return ItemType.Electronics;
        }

        String normalized = rawType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ART" -> ItemType.Art;
            case "VEHICLE" -> ItemType.Vehicle;
            default -> ItemType.Electronics;
        };
    }

    private static String deriveItemName(ItemType itemType, String itemInfo, int requestId) {
        if (itemInfo != null && !itemInfo.isBlank()) {
            String firstLine = itemInfo.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .findFirst()
                    .orElse("");
            if (!firstLine.isBlank()) {
                return firstLine.length() > 60 ? firstLine.substring(0, 60) : firstLine;
            }
        }
        return itemType.name() + " Request #" + requestId;
    }

    public record PendingItemRequest(
            int requestId,
            String userId,
            ItemType itemType,
            String derivedItemName,
            String itemInfo,
            double basePrice,
            double bidIncrement
    ) {
        @Override
        public String toString() {
            return "[" + itemType + "] " + derivedItemName
                    + " | User: " + userId
                    + " | Base: " + basePrice
                    + " | Increment: " + bidIncrement
                    + " | " + itemInfo;
        }
    }
}
