package backends.client.controllers.components;

import backends.common.models.core.Item;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class ItemPreviewCell extends ListCell<Item> {
    private final HBox content;
    private final Label nameLabel;
    private final Label metaLabel;
    private final Button viewButton;
    private final Consumer<Item> onView;

    public ItemPreviewCell(Consumer<Item> onView) {
        this.onView = onView;

        nameLabel = new Label();
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        nameLabel.setWrapText(true);

        metaLabel = new Label();
        metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        metaLabel.setWrapText(true);
        metaLabel.setMaxWidth(210);

        viewButton = new Button("View");
        viewButton.setMinWidth(62);
        viewButton.setPrefHeight(30);
        viewButton.setStyle("-fx-background-color: #ea580c; -fx-text-fill: white; -fx-background-radius: 12; -fx-font-weight: bold;");
        viewButton.setOnAction(event -> {
            Item item = getItem();
            if (item != null && this.onView != null) {
                this.onView.accept(item);
            }
        });

        VBox infoBox = new VBox(4, nameLabel, metaLabel);
        infoBox.setFillWidth(true);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        content = new HBox(10, infoBox, spacer, viewButton);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(12, 12, 12, 12));
        content.setMinHeight(88);
        content.setPrefHeight(88);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
    }

    @Override
    protected void updateItem(Item item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        nameLabel.setText(item.getName() != null ? item.getName() : "Unknown");
        metaLabel.setText(
                "Type: " + item.getType() + "\n" +
                        "Opening: " + String.format("%,.0f", item.getPrices()) + " VND\n" +
                        "ID: " + item.getId()
        );
        setGraphic(content);
    }
}
