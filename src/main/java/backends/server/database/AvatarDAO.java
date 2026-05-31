package backends.server.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class AvatarDAO {
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path AVATAR_DIRECTORY = DATA_DIRECTORY.resolve("avatars");
    private static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("app.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS user_avatar (
                user_id TEXT PRIMARY KEY,
                image_path TEXT NOT NULL
            )
            """;

    public AvatarDAO() {
        try {
            initializeStorage();
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Khong the khoi tao co so du lieu avatar.", e);
        }
    }

    public synchronized void saveAvatar(String userId, String imagePath) throws IOException {
        // Chỉ lưu metadata: user nào có avatar nào trên server.
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO user_avatar (user_id, image_path)
                     VALUES (?, ?)
                     ON CONFLICT(user_id) DO UPDATE SET image_path = excluded.image_path
                     """)) {
            statement.setString(1, userId);
            statement.setString(2, imagePath);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu avatar vao SQLite.", e);
        }
    }

    public Optional<String> getAvatarPath(String userId) throws IOException {
        // Lấy path tương đối của file avatar đã lưu trên server.
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT image_path
                     FROM user_avatar
                     WHERE user_id = ?
                     LIMIT 1
                     """)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(resultSet.getString("image_path"));
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay avatar tu SQLite.", e);
        }
    }

    private void initializeStorage() throws IOException, SQLException {
        ensureDataDirectoryExists();
        ensureAvatarDirectoryExists();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE_SQL);
        }
    }

    private void ensureDataDirectoryExists() throws IOException {
        if (Files.notExists(DATA_DIRECTORY)) {
            Files.createDirectories(DATA_DIRECTORY);
        }
    }

    public Path getAvatarDirectory() throws IOException {
        // Thư mục vật lý chứa file avatar thật trên server.
        ensureAvatarDirectoryExists();
        return AVATAR_DIRECTORY;
    }

    private void ensureAvatarDirectoryExists() throws IOException {
        if (Files.notExists(AVATAR_DIRECTORY)) {
            Files.createDirectories(AVATAR_DIRECTORY);
        }
    }

    private Connection openConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        return DriverManager.getConnection(DATABASE_URL);
    }
}
