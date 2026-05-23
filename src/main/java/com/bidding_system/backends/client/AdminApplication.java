package backends.client;

import backends.client.session.UserSession;
import backends.client.controllers.ViewLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import backends.launcher.Launcher;

public class AdminApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        try {
            UserSession.initConnection(Launcher.serverIp, 9999);
            Scene scene = new Scene(ViewLoader.load("AdminProfile.fxml"));
            stage.setTitle("Thong tin admin");
            stage.setScene(scene);
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
