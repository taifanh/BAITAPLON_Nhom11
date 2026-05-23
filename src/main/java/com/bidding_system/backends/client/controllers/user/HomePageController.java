package backends.client.controllers.user;

import backends.client.controllers.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class HomePageController {
    @FXML
    public void openProfile(ActionEvent event) throws IOException {
        openView(event, "UserInfo.fxml", "User Profile");
    }

    @FXML
    public void openBiddingSpace(ActionEvent event) throws IOException {
        openView(event, "BiddingSpace.fxml", "Bidding Space");
    }

    @FXML
    public void openSellItem(ActionEvent event) throws IOException {
        openView(event, "SellItem.fxml", "Sell Item");
    }

    @FXML
    public void openHistory(ActionEvent event) throws IOException {
        Parent root = ViewLoader.load("History.fxml");
        switchScene(event, root, "Transaction History");
    }

    private void openView(ActionEvent event, String viewFileName, String title) throws IOException {
        Parent root = ViewLoader.load(viewFileName);
        switchScene(event, root, title);
    }

    private void switchScene(ActionEvent event, Parent root, String title) {
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.setTitle(title);
        fitToVisibleScreen(window);
        window.show();
    }

    private void fitToVisibleScreen(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setMaximized(false);
        stage.setMinWidth(1000);
        stage.setMinHeight(620);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }
}
