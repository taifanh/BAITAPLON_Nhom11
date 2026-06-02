package backends.launcher;

import backends.common.models.accounts.Admin;
import backends.server.ServerApplication;
import backends.server.database.UserDAO;

import java.io.Console;
import java.io.IOException;
import java.util.Scanner;

public class ServerLauncher {
    public static String serverIp = "localhost";

    public static void main(String[] args) {
        ServerStart();
    }

    public static void ServerStart() {
        UserDAO userDAO = new UserDAO();
        try {
            if (!userDAO.adminExists()) {
                System.out.println("=== KHỞI TẠO TÀI KHOẢN ADMIN ===");
                System.out.println("Chưa có tài khoản admin trong hệ thống. Vui lòng tạo mới.\n");

                Scanner scanner = new Scanner(System.in);

                System.out.print("Nhập tên admin: ");
                String name = scanner.nextLine().trim();

                System.out.print("Nhập email: ");
                String email = scanner.nextLine().trim();

                System.out.print("Nhập số điện thoại: ");
                String phone = scanner.nextLine().trim();

                System.out.print("Nhập mật khẩu: ");
                String password = scanner.nextLine().trim();

                if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                    System.err.println("[ServerLauncher] Tên, số điện thoại và mật khẩu không được để trống. Server dừng lại.");
                    return;
                }

                Admin admin = Admin.creating_admin(name, email, password, phone);
                userDAO.saveAdmin(admin);
                System.out.println("[ServerLauncher] Tài khoản admin đã được tạo thành công!\n");
            } else {
                System.out.println("[ServerLauncher] Admin đã tồn tại trong hệ thống, bỏ qua bước tạo admin.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi kiểm tra/tạo tài khoản admin.", e);
        }

        Thread serverThread = new Thread(() -> {
            System.out.println("[ServerLauncher] Khởi động Server tại IP: " + GetExactIP.getIP());
            ServerApplication.start();
        });
        serverThread.start();
    }
}