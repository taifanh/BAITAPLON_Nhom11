package com.bidding_system.backends.common.messages.Common;

public enum MessageType {
    // ======== User Authentication ========
    SIGNIN("signin"),
    SIGNUP("signup"),
    
    // ======== User Account ========
    GET_BALANCE("GET_BALANCE"),
    DEPOSIT("DEPOSIT"),
    CHANGE_INFO("change_info"),
    
    // ======== Item Management ========
    ADDITEM("additem"),
    REMOVEITEM("removeitem"),
    
    // ======== Auction Operations ========
    AUCTION_ITEMS_RESPONSE("AUCTION_ITEMS_RESPONSE"),
    GET_AUCTIONS("GET_AUCTIONS"),
    AUCTION_COMMAND("AUCTION_COMMAND"),
    FETCH_AUCTION_STATUS("FETCH_AUCTION_STATUS"),
    PLACE_BID("PLACE_BID"),
    WATCH_AUCTION("WATCH_AUCTION"),
    UNWATCH_AUCTION("UNWATCH_AUCTION"),
    REGISTER_AUTO_BIDDING("REGISTER_AUTO_BIDDING"),
    CANCEL_AUTO_BIDDING("CANCEL_AUTO_BIDDING"),
    
    // ======== Admin Operations ========
    FETCH_INVENTORY("FETCH_INVENTORY"),
    FETCH_BID_HISTORY("FETCH_BID_HISTORY"),
    FETCH_REQUESTS("FETCH_REQUESTS"),
    ADMIN_ACTION("ADMIN_ACTION");
    
    private final String value;
    
    MessageType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static MessageType fromValue(String value) {
        for (MessageType type : MessageType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
