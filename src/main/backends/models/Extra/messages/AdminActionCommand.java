package models.Extra.messages;

public class AdminActionCommand {
    public String type = "ADMIN_ACTION";
    public String action; // "ACCEPT_REQUEST", "REJECT_REQUEST", "SCHEDULE_ITEM"
    public String targetId; // Có thể là requestId hoặc itemId

    public AdminActionCommand() {}

    public AdminActionCommand(String action, String targetId) {
        this.action = action;
        this.targetId = targetId;
    }
}