package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.accounts.User;

import java.io.IOException;
import java.util.Optional;

public class depositcontroller {
    @FXML
    public TextField deposit_amount;
    @FXML
    public Button ok_deposit;
    @FXML
    public Button verifybtn;
    @FXML
    public TextField name_deposit_input;
    @FXML
    public TextField phonenumber_deposit_input;
    @FXML
    public Label money_display;


}
