package com.bidding_system.backends.client.controllers.components;

import com.bidding_system.backends.common.messages.MsgData.BidHistoryRecordDto;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CustomBidHistoryCell extends TableCell<BidHistoryRow, String> {
    private static final String TEXT_STYLE =
            "-fx-padding: 8 10 8 10; -fx-text-fill: #0f172a;";
    private static final String AMOUNT_STYLE =
            "-fx-padding: 8 10 8 10; -fx-alignment: CENTER_RIGHT; -fx-text-fill: #c2410c; -fx-font-weight: bold;";
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final boolean amountCell;

    public CustomBidHistoryCell(boolean amountCell) {
        this.amountCell = amountCell;
    }

    @Override
    protected void updateItem(String value, boolean empty) {
        super.updateItem(value, empty);
        setText(empty ? null : value);
        if (empty) {
            setTextFill(null);
            setStyle("");
        } else {
            setTextFill(amountCell ? Color.web("#c2410c") : Color.web("#0f172a"));
            setStyle(amountCell ? AMOUNT_STYLE : TEXT_STYLE);
        }
    }

    public static void configureTable(TableView<BidHistoryRow> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No bids yet."));
        table.getColumns().setAll(
                createTextColumn("#", "sequence", 54),
                createTextColumn("Bidder", "bidder", 170),
                createAmountColumn(),
                createTextColumn("Time", "time", 110)
        );
    }

    public static BidHistoryRow toRow(int index, BidHistoryRecordDto record) {
        return toRow(index, record.bidderName, record.bidderId, record.amount, TIME_FORMATTER.format(record.bidTime));
    }

    public static BidHistoryRow toRow(int index, String bidderName, String bidderId, double amount, Instant bidTime) {
        return toRow(index, bidderName, bidderId, amount, TIME_FORMATTER.format(bidTime));
    }

    public static BidHistoryRow toRow(int index, String bidderName, String bidderId, double amount, String time) {
        return new BidHistoryRow(
                String.valueOf(index),
                bidderName + " (" + bidderId + ")",
                String.valueOf(amount),
                time
        );
    }

    private static TableColumn<BidHistoryRow, String> createTextColumn(String title, String property, double width) {
        TableColumn<BidHistoryRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        column.setCellFactory(col -> new CustomBidHistoryCell(false));
        return column;
    }

    private static TableColumn<BidHistoryRow, String> createAmountColumn() {
        TableColumn<BidHistoryRow, String> column = new TableColumn<>("Amount");
        column.setCellValueFactory(new PropertyValueFactory<>("amount"));
        column.setPrefWidth(110);
        column.setCellFactory(col -> new CustomBidHistoryCell(true));
        return column;
    }
}
