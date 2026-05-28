package backends.client.controllers.user;

import backends.client.controllers.base.BaseController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

public class HomePageController extends BaseController {
    @Override
    public void cleanup() {
    }

    @FXML
    public void openProfile(ActionEvent event) throws IOException {
        openView(event, "UserProfile.fxml", "User Profile");
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
        switchScene(event, "History.fxml", "Transaction History");
    }

    private void openView(ActionEvent event, String viewFileName, String title) throws IOException {
        switchScene(event, viewFileName, title);
    }
}
