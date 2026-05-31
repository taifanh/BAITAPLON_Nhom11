package backends.client.controllers.components;

import backends.common.messages.MsgData.ItemRecordDto;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDateTime;

public class BuyerItemCell extends ListCell<ItemRecordDto> {
    private final HBox content;
    private final Label itemNameLabel;
    private final Label itemTypeLabel;
    private final Label descriptionLabel;
    private final Label priceLabel;
    private final Label timeLabel;

    public BuyerItemCell() {
        super();

        itemNameLabel = new Label();
        itemNameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        itemTypeLabel = new Label();
        itemTypeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ea580c; -fx-font-weight: bold;");

        descriptionLabel = new Label();
        descriptionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-wrap-text: true;");

        priceLabel = new Label();
        priceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ea580c;");

        timeLabel = new Label();
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // Tạo layout cho thông tin chính
        VBox mainInfoBox = new VBox(4, itemNameLabel, itemTypeLabel, descriptionLabel);
        mainInfoBox.setMaxWidth(400);

        // Tạo layout cho giá và trạng thái
        VBox priceStatusBox = new VBox(4, priceLabel);

        // Tạo layout cho thời gian
        VBox timeBox = new VBox(4, timeLabel);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        content = new HBox(20, mainInfoBox, spacer, priceStatusBox, timeBox);
        content.setPadding(new Insets(12, 16, 12, 16));
        content.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
    }

    @Override
    protected void updateItem(ItemRecordDto item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null && !empty) {
            itemNameLabel.setText(item.itemName != null ? item.itemName : "Unknown");

            itemTypeLabel.setText("Loại: " + (item.itemType != null ? item.itemType : "N/A"));

            String description = item.itemInfo != null ? item.itemInfo : "Không có mô tả";
            if (description.length() > 100) {
                description = description.substring(0, 100) + "...";
            }
            descriptionLabel.setText("Mô tả: " + description);

            priceLabel.setText(String.format("Giá: %,.0f VND", item.bidPrice));

            String timeText = formatAuctionTime(item.startAt, item.endAt);
            timeLabel.setText(timeText);

            setGraphic(content);
        } else {
            setGraphic(null);
        }
    }

    private String formatAuctionTime(String startAt, String endAt) {
        try {
            if (startAt != null && !startAt.isBlank() && endAt != null && !endAt.isBlank()) {
                LocalDateTime start = LocalDateTime.parse(startAt);
                LocalDateTime end = LocalDateTime.parse(endAt);
                Duration duration = Duration.between(start, end);
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                return String.format("Thời gian: %s → %s (%dh %dm)",
                        start.toLocalTime().toString(),
                        end.toLocalTime().toString(),
                        hours, minutes);
            }
        } catch (Exception ignored) {
        }
        return "Thời gian: " + (startAt != null ? startAt : "") + " → " + (endAt != null ? endAt : "");
    }
}

