package backends.client.controllers.admin;

import backends.client.controllers.base.BaseController;
import backends.client.session.UserSession;
import backends.common.models.core.Account;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import backends.client.controllers.ViewLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class AdminProfileController extends BaseController {

    // ── TabPane ───────────────────────────────────────────────────
    @FXML private TabPane adminTabs;
    @FXML private Tab adminProfileTab;
    @FXML private Tab adminRequestsTab;
    @FXML private Tab adminInventoryTab;
    @FXML private Tab adminBiddingTab;

    // ── Nav buttons ───────────────────────────────────────────────
    @FXML private Button profileNavButton;
    @FXML private Button requestsNavButton;
    @FXML private Button inventoryNavButton;
    @FXML private Button biddingNavButton;

    private static final String STYLE_ACTIVE =
            "-fx-background-color:#ea580c;-fx-background-radius:14;" +
                    "-fx-text-fill:white;-fx-font-weight:bold;";
    private static final String STYLE_INACTIVE =
            "-fx-background-color:transparent;-fx-border-color:#ea580c;" +
                    "-fx-border-radius:14;-fx-background-radius:14;" +
                    "-fx-text-fill:#ea580c;-fx-font-weight:bold;";

    // ── Sub-controllers (requests / inventory / bidding) ──────────
    @FXML private AdminRequestController   requestsPaneController;
    @FXML private AdminInventoryController inventoryPaneController;
    @FXML private AdminBiddingController   biddingPaneController;

    // ── Profile fields (từ AdminProfile.fxml) ────────────────────────
    @FXML private TextField labelName;
    @FXML private Label     labelPhoneNumber;
    @FXML private Label     labelEmail;
    @FXML private Label     labelPassword;
    @FXML private CheckBox  checkShowPassword;

    private String rawPassword = "";


    @FXML
    public void initialize() {
        // Tab navigation
        adminTabs.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, tab) -> refreshNavStyles());
        refreshNavStyles();

        // Bind profile data
        Account account = UserSession.getCurrentAccount();
        if (account != null) {
            labelName.setText(account.getName());
            labelPhoneNumber.setText(account.getPhoneNumber());
            labelEmail.setText(account.getEmail());
            rawPassword = account.getPassword() == null ? "" : account.getPassword();
            labelPassword.setText(masked(rawPassword));
        }

        // Show/hide password toggle
        checkShowPassword.selectedProperty().addListener((obs, old, isSelected) ->
                labelPassword.setText(isSelected ? rawPassword : masked(rawPassword))
        );

    }

    // ── Tab navigation ────────────────────────────────────────────

    @FXML
    public void showProfile(ActionEvent event) {
        adminTabs.getSelectionModel().select(adminProfileTab);
        updateNavButtonStyles();
    }

    @FXML
    public void showRequests(ActionEvent event) {
        adminTabs.getSelectionModel().select(adminRequestsTab);
        updateNavButtonStyles();
    }

    @FXML
    public void showInventory(ActionEvent event) {
        adminTabs.getSelectionModel().select(adminInventoryTab);
        updateNavButtonStyles();
    }

    @FXML
    public void showBidding(ActionEvent event) {
        adminTabs.getSelectionModel().select(adminBiddingTab);
        updateNavButtonStyles();
    }

    private void updateNavButtonStyles() {
        if (adminTabs == null) {
            return;
        }

        Tab selectedTab = adminTabs.getSelectionModel().getSelectedItem();
        setNavButtonActive(profileNavButton, selectedTab == adminProfileTab);
        setNavButtonActive(requestsNavButton, selectedTab == adminRequestsTab);
        setNavButtonActive(inventoryNavButton, selectedTab == adminInventoryTab);
        setNavButtonActive(biddingNavButton, selectedTab == adminBiddingTab);
    }

    private void setNavButtonActive(Button button, boolean active) {
        if (button != null) {
            button.setStyle(active ? STYLE_ACTIVE : STYLE_INACTIVE);
        }
    }

    @FXML
    public void handleSignOut(ActionEvent e) {
        UserSession.setCurrentAccount(null);
        navigate("SignIn.fxml", "Dang nhap", e.getSource());
    }

    private void navigate(String fxml, String title, Object source) {
        try {
            Parent root = ViewLoader.load(fxml);
            Stage stage = (Stage) ((Node) source).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void cleanup() {
        if (requestsPaneController  != null) requestsPaneController.cleanup();
        if (inventoryPaneController != null) inventoryPaneController.cleanup();
        if (biddingPaneController   != null) biddingPaneController.cleanup();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void select(Tab tab) {
        adminTabs.getSelectionModel().select(tab);
        refreshNavStyles();
    }

    private void refreshNavStyles() {
        Tab sel = adminTabs.getSelectionModel().getSelectedItem();
        setActive(profileNavButton,   sel == adminProfileTab);
        setActive(requestsNavButton,  sel == adminRequestsTab);
        setActive(inventoryNavButton, sel == adminInventoryTab);
        setActive(biddingNavButton,   sel == adminBiddingTab);
    }

    private void setActive(Button btn, boolean active) {
        if (btn != null) btn.setStyle(active ? STYLE_ACTIVE : STYLE_INACTIVE);
    }

    private String masked(String pw) {
        return "●".repeat(Math.max(0, pw.length()));
    }
}