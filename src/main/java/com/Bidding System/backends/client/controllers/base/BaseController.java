package backends.client.controllers.base;

import backends.client.controllers.ViewLoader;
import backends.client.network.MessageBus;
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
     * Mỗi subclass tự unsubscribe MessageBus handlers,
     * stop Timeline, giải phóng tài nguyên.
     */
    public abstract void cleanup();

    // ── Sign out ──────────────────────────────────────────────────

    @FXML
    public void handleSignOut(ActionEvent event) throws IOException {
        cleanup();
        MessageBus.getInstance().clearAllSubscribers();
        UserSession.clear();

        Stage window = getStage(event);
        Parent root  = ViewLoader.load("SignIn.fxml");
        resetToLoginSize(window);
        window.setScene(new Scene(root));
        window.setTitle("Sign In");
        window.centerOnScreen();
        window.show();
    }

    // ── Navigation (User screens) ─────────────────────────────────

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

    // ── Shared utilities ──────────────────────────────────────────

    protected void switchScene(ActionEvent event, String fxml, String title)
            throws IOException {
        cleanup();
        Parent root   = ViewLoader.load(fxml);
        Stage  window = getStage(event);
        window.setScene(new Scene(root));
        window.setTitle(title);
        fitToScreen(window);
        window.show();
    }

    protected Stage getStage(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }

    protected void fitToScreen(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setMinWidth(1000);
        stage.setMinHeight(620);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    protected void resetToLoginSize(Stage window) {
        window.setFullScreen(false);
        window.setMaximized(false);
        window.setMinWidth(0);
        window.setMinHeight(0);
        window.setWidth(450);
        window.setHeight(500);
    }
}