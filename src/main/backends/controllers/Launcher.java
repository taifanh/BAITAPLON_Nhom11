package controllers;

import controllers.Server.ServerLauncher;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // 1. Tạo luồng cho Server
        Thread serverThread = new Thread(() -> {
            System.out.println("[Launcher] Đang khởi động Server...");
            // Gọi phương thức main của ServerLauncher
            ServerLauncher.start();
        });

        // 2. Tạo luồng cho Client
        Thread clientThread = new Thread(() -> {
            System.out.println("[Launcher] Đang khởi động Client...");
            // Gọi phương thức main của ClientLauncher
            Application.launch(ClientLauncher.class, args);
        });

        // 3. Kích hoạt Server chạy trước
        serverThread.start();

        // 4. Tạm dừng luồng chính một chút (Delay)
        // để chắc chắn Server đã mở cổng 8080 thành công
        try {
            System.out.println("[Launcher] Đợi Server sẵn sàng...");
            Thread.sleep(2000); // Đợi 2 giây
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 5. Sau khi đợi, mới kích hoạt Client
        clientThread.start();
    }
}
