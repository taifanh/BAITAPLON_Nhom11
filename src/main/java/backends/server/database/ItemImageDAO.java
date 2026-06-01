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

public class ItemImageDAO {
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path ITEM_IMAGE_DIRECTORY = DATA_DIRECTORY.resolve("itemImage");
    private static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("app.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS item_image (
                request_id TEXT PRIMARY KEY,
                item_id TEXT,
                image_path TEXT NOT NULL
            )
            """;

    public ItemImageDAO() {
        try {
            initializeStorage();
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Khong the khoi tao co so du lieu anh san pham.", e);
        }
    }

    public synchronized void saveImage(String requestId, String itemId, String imagePath) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO item_image (request_id, item_id, image_path)
                     VALUES (?, ?, ?)
                     ON CONFLICT(request_id) DO UPDATE SET
                         item_id = excluded.item_id,
                         image_path = excluded.image_path
                     """)) {
            statement.setString(1, requestId);
            statement.setString(2, itemId);
            statement.setString(3, imagePath);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu thong tin anh san pham.", e);
        }
    }

    public synchronized void updateItemId(String requestId, String itemId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE item_image
                     SET item_id = ?
                     WHERE request_id = ?
                     """)) {
            statement.setString(1, itemId);
            statement.setString(2, requestId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the cap nhat item_id cho anh san pham.", e);
        }
    }

    public Optional<ImageRecord> findByItemId(String itemId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT request_id, item_id, image_path
                     FROM item_image
                     WHERE item_id = ?
                     LIMIT 1
                     """)) {
            statement.setString(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ImageRecord(
                        resultSet.getString("request_id"),
                        resultSet.getString("item_id"),
                        resultSet.getString("image_path")
                ));
            }
        } catch (SQLException e) {
            throw new IOException("Khong the tim anh theo item_id.", e);
        }
    }

    public Optional<ImageRecord> findByRequestId(String requestId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT request_id, item_id, image_path
                     FROM item_image
                     WHERE request_id = ?
                     LIMIT 1
                     """)) {
            statement.setString(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ImageRecord(
                        resultSet.getString("request_id"),
                        resultSet.getString("item_id"),
                        resultSet.getString("image_path")
                ));
            }
        } catch (SQLException e) {
            throw new IOException("Khong the tim anh theo request_id.", e);
        }
    }

    public synchronized void deleteByRequestId(String requestId) throws IOException {
        Optional<ImageRecord> existing = findByRequestId(requestId);
        if (existing.isEmpty()) {
            return;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM item_image
                     WHERE request_id = ?
                     """)) {
            statement.setString(1, requestId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the xoa anh san pham.", e);
        }

        deletePhysicalFile(existing.get().imagePath());
    }

    public Path getItemImageDirectory() throws IOException {
        ensureItemImageDirectoryExists();
        return ITEM_IMAGE_DIRECTORY;
    }

    public Path resolveStoredPath(String imagePath) {
        Path path = Path.of(imagePath);
        if (path.isAbsolute()) {
            return path;
        }
        return Path.of("data").resolve(path);
    }

    private void deletePhysicalFile(String imagePath) throws IOException {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        Path storedPath = resolveStoredPath(imagePath);
        Files.deleteIfExists(storedPath);
    }

    private void initializeStorage() throws IOException, SQLException {
        ensureDataDirectoryExists();
        ensureItemImageDirectoryExists();
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

    private void ensureItemImageDirectoryExists() throws IOException {
        if (Files.notExists(ITEM_IMAGE_DIRECTORY)) {
            Files.createDirectories(ITEM_IMAGE_DIRECTORY);
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

    public record ImageRecord(String requestId, String itemId, String imagePath) {
    }
}
