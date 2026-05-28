package backends.client.controllers.base;

import backends.client.controllers.ViewLoader;
import backends.client.network.MessageBus;
import backends.client.session.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public abstract class BaseController {
    public static final double LOGIN_WIDTH = 450;
    public static final double LOGIN_HEIGHT = 540;
    public static final double MAIN_WIDTH = 1366;
    public static final double MAIN_HEIGHT = 720;

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
        Scene loginScene = new Scene(root, LOGIN_WIDTH, LOGIN_HEIGHT);

        window.setFullScreen(false);
        window.setMaximized(false);
        window.setResizable(false);
        window.setScene(loginScene);
        window.setTitle("Sign In");
        window.setWidth(LOGIN_WIDTH);
        window.setHeight(LOGIN_HEIGHT);
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
        switchScene(event, "UserProfile.fxml", "User Profile");
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
        window.setFullScreen(false);
        window.setMaximized(false);
        window.setResizable(false);
        window.setScene(new Scene(root, MAIN_WIDTH, MAIN_HEIGHT));
        window.setTitle(title);
        window.setWidth(MAIN_WIDTH);
        window.setHeight(MAIN_HEIGHT);
        window.centerOnScreen();
        window.show();
    }

    protected Stage getStage(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }

}
