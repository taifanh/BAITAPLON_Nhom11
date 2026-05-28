package com.bidding_system.backends.client;

import com.bidding_system.backends.client.session.UserSession;
import com.bidding_system.backends.client.controllers.ViewLoader;
import com.bidding_system.backends.client.controllers.base.BaseController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.bidding_system.backends.launcher.Launcher;

public class AdminApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        try {
            UserSession.initConnection(Launcher.serverIp, 9999);
            Scene scene = new Scene(ViewLoader.load("AdminProfile.fxml"), BaseController.MAIN_WIDTH, BaseController.MAIN_HEIGHT);
            stage.setTitle("Thong tin admin");
            stage.setResizable(false);
            stage.setScene(scene);
            stage.setWidth(BaseController.MAIN_WIDTH);
            stage.setHeight(BaseController.MAIN_HEIGHT);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("[AdminApplication] Failed to start");
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Đang đóng chương trình Admin...");
        System.exit(0);
    }
}
