package backends.client.controllers.base;

import backends.client.controllers.ViewLoader;
import backends.client.session.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public abstract class BaseController {

    /**
     * Subclass override để unsubscribe MessageBus handlers,
     * stop timelines, v.v. trước khi chuyển màn hình.
     */
    public abstract void cleanup();

    @FXML
    public void handleHome(ActionEvent event) throws IOException {
        switchScene(event, "HomePage.fxml", "Home");
    }

    @FXML
    public void openProfile(ActionEvent event) throws IOException {
        switchScene(event, "UserInfo.fxml", "User Profile");
    }

    @FXML
    public void openBiddingSpace(ActionEvent event) throws IOException {
        switchScene(event, "BiddingSpace.fxml", "Bidding Space");
    }

    @FXML
    public void openSellItem(ActionEvent event) throws IOException {
        switchScene(event, "SellItem.fxml", "Sell Item");
    }

    @FXML
    public void openHistory(ActionEvent event) throws IOException {
        switchScene(event, "History.fxml", "Transaction History");
    }

    @FXML
    public void handleSignOut(ActionEvent event) throws IOException {
        cleanup();
        UserSession.clear();
        Parent root = ViewLoader.load("SignIn.fxml");
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        resetToLoginSize(window);
        window.setScene(new Scene(root));
        window.setTitle("Sign In");
        window.centerOnScreen();
        window.show();
    }

    protected void switchScene(ActionEvent event, String fxml, String title)
            throws IOException {
        cleanup();
        Parent root = ViewLoader.load(fxml);
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(new Scene(root));
        window.setTitle(title);
        fitToScreen(window);
        window.show();
    }

    private void fitToScreen(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setMinWidth(1000);
        stage.setMinHeight(620);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    private void resetToLoginSize(Stage window) {
        window.setFullScreen(false);
        window.setMaximized(false);
        window.setMinWidth(0);
        window.setMinHeight(0);
        window.setWidth(450);
        window.setHeight(500);
    }
}