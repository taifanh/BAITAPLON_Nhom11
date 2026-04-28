package Database;

import models.accounts.Admin;
import models.accounts.User;
import models.core.Account;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserStore {
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("users.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_USERS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                phone_number TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL,
                password TEXT NOT NULL,
                balance DOUBLE,
                role TEXT NOT NULL
            )
            """;

    public UserStore() {
        try {
            initializeStorage();
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Khong the khoi tao co so du lieu SQLite.", e);
        }
    }

    public User getUser(String id) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, email , phone_number, password
                     FROM users
                     where id = ?
                     """)) {

             statement.setString(1, id);
             try(ResultSet resultSet = statement.executeQuery()) {
                 resultSet.next();
                 User user = new User(
                         resultSet.getString("id"),
                         resultSet.getString("name"),
                         resultSet.getString("email"),
                         resultSet.getString("phone_number"),
                         resultSet.getString("password")
                 );
                 return user;
             }
        } catch (SQLException e) {
            throw new IOException("Khong co user voi id = " + id, e);
        }
    }

    public List<User> getAllUsers() throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, email , phone_number, password
                     FROM users
                     ORDER BY rowid
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                User user = new User(
                        resultSet.getString("id"),
                        resultSet.getString("name"),
                        resultSet.getString("email"),
                        resultSet.getString("phone_number"),
                        resultSet.getString("password")
                );
                user.setId(resultSet.getString("id"));
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            throw new IOException("Khong the doc du lieu nguoi dung tu SQLite.", e);
        }
    }

    //kiểm tra role của account để trả về User hay Admin
    public Optional<Account> authenticate(String phoneNumber, String password) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, email , phone_number, password , balance, role
                     FROM users
                     WHERE phone_number = ? AND password = ?
                     LIMIT 1
                     """)) {
            statement.setString(1, phoneNumber);
            statement.setString(2, password);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                String role = resultSet.getString("role");
                Account account;

                if ("Admin".equalsIgnoreCase(role)) {
                    account = new Admin(
                            resultSet.getString("id"),
                            resultSet.getString("name"),
                            resultSet.getString("email"),
                            resultSet.getString("phone_number"),
                            resultSet.getString("password")
                    );
                } else {
                    account = new User(
                            resultSet.getString("id"),
                            resultSet.getString("name"),
                            resultSet.getString("email"),
                            resultSet.getString("phone_number"),
                            resultSet.getString("password"),
                            resultSet.getDouble("balance")
                    );
                }

                account.setId(resultSet.getString("id"));
                account.setRole(role);
                return Optional.of(account);
            }
        } catch (SQLException e) {
            throw new IOException("Khong the xac thuc nguoi dung tu SQLite.", e);
        }
    }

    public boolean phoneNumberExists(String phoneNumber) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT 1
                     FROM users
                     WHERE phone_number = ? 
                     LIMIT 1
                     """)) {
            statement.setString(1, phoneNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new IOException("Khong the kiem tra so dien thoai trong SQLite.", e);
        }
    }

    public void saveUser(User user) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO users (id, name, email ,phone_number, password , balance,role)
                     VALUES (?, ?, ?, ?, ? , ? , ?)
                     """)) {
            statement.setString(1, user.getId());
            statement.setString(2, user.getName());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPhoneNumber());
            statement.setString(5, user.getPassword());
            statement.setDouble(6,0.0);
            statement.setString(7,"User");
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu nguoi dung vao SQLite.", e);
        }
    }
    public void saveAdmin(Admin admin) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO users (id, name, email ,phone_number, password , balance,role)
                     VALUES (?, ?, ?, ?, ? , ? , ?)
                     """)) {
            statement.setString(1, admin.getId());
            statement.setString(2, admin.getName());
            statement.setString(3, admin.getEmail());
            statement.setString(4, admin.getPhoneNumber());
            statement.setString(5, admin.getPassword());
            statement.setDouble(6,0.0);
            statement.setString(7,"Admin");
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu nguoi dung vao SQLite.", e);
        }
    }
    public void update_balance(double new_balance, String userId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE users
                     SET balance = balance + ?
                     WHERE id = ?
                     """)) {
            statement.setDouble(1, new_balance);
            statement.setString(2,userId);
            try {
                statement.executeUpdate();
            }catch (SQLException e) {
                throw new IOException("Khong the cập nhật balance trong SQLite.", e);
            }
        } catch (SQLException e) {
            throw new IOException("Khong the cập nhật balance trong SQLite.", e);
        }
    }
    public void change_info(String new_name , String new_email , String new_phonenumber ,String new_password ,  String id) throws IOException {
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
                  UPDATE users
                  SET name = ?, email = ?, phone_number = ?, password = ?
                  WHERE id = ?
""")) {
            statement.setString(1, new_name);
            statement.setString(2, new_email);
            statement.setString(3, new_phonenumber);
            statement.setString(4, new_password);
            statement.setString(5, id );

            try{
                statement.executeUpdate();
            } catch(SQLException e) {
                throw new IOException("khong the thay doi thong tin. " ,e );
            }
        } catch (SQLException e) {
            throw new IOException("khong the thay doi thong tin." ,e);
        }
    }

    private void initializeStorage() throws IOException, SQLException {
        ensureDataDirectoryExists();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_USERS_TABLE_SQL);
        }
    }

    private void ensureDataDirectoryExists() throws IOException {
        if (Files.notExists(DATA_DIRECTORY)) {
            Files.createDirectories(DATA_DIRECTORY);
        }
    }
    private boolean looksLikeEmail(String value) {
        return value != null && value.contains("@");
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }
}
