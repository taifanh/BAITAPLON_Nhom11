package controllers;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientLauncher extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        UserSession.initConnection("localhost", 9999);
        Scene scene = new Scene(ViewLoader.load("signin.fxml"), 500, 500);
        stage.setTitle("sign in!");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}
