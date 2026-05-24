package com.bidding_system.backends.common.messages.MsgAuction;

import com.bidding_system.backends.server.database.RequestLogDAO;

import java.io.IOException;

public class AdminActionCommand {
    public String type = "ADMIN_ACTION";
    public String action; // "ACCEPT_REQUEST", "REJECT_REQUEST", "SCHEDULE_ITEM"
    public String targetId; // Có thể là requestId hoặc itemId
    public String userId;

    public AdminActionCommand() {}

    public AdminActionCommand(String action, String targetId) throws IOException {
        this.action = action;
        this.targetId = targetId;
    }
    public AdminActionCommand(String action , String targetId, String userId) throws IOException {// dùng cho các trường hợp đặc biệt cần thông tin cụ thể hơn
        this.action = action;
        this.targetId = targetId;
        this.userId = userId;
    }
    public void SetuserId(String targetId) throws IOException {
        RequestLogDAO requestlog =  new RequestLogDAO();
        this.userId = requestlog.getUserbyRequestId(targetId);// dùng cho khi muốn lấy userid từ request id
    }
}
