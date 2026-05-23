package backends.client;

import backends.client.session.UserSession;
import backends.client.controllers.ViewLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import backends.launcher.Launcher;

public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        try {
            UserSession.initConnection(Launcher.serverIp, 9999);
            Scene scene = new Scene(ViewLoader.load("SignIn.fxml"));
            stage.setTitle("sign in!");
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("[ClientApplication] Failed to start");
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Đang đóng chương trình User...");
        UserSession.shutdown();
        System.exit(0);
    }
}
