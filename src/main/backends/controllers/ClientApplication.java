package controllers;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import Launcher.Launcher;

public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        UserSession.initConnection(Launcher.serverIp, 9999);
        Scene scene = new Scene(ViewLoader.load("signin.fxml"), 500, 500);
        stage.setTitle("sign in!");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Đang đóng chương trình...");
        System.exit(0);
    }
}
