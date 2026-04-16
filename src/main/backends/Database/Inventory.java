package Database;

import models.core.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Inventory {
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("inventory.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_INVENTORY_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS inventory (
                ItemId TEXT PRIMARY KEY,
                type TEXT NOT NULL,
                name TEXT NOT NULL,
                price DOUBLE,
                itemDescription TEXT,
                userId TEXT,
                status VARCHAR(20)
            )
            """;

    public Inventory() throws IOException {
        try {
            initializeStorage();
        } catch (SQLException e) {
            throw new IOException("Khong the khoi tao bang inventory", e);
        }
    }

    public void saveItem(Item item, String userId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO inventory(ItemId,type,name,price,itemDescription,userId,status)
                     VALUES(?,?,?,?,?,?,?)
                     """)) {
            statement.setString(1, item.getId());
            statement.setString(2, item.getType());
            statement.setString(3, item.getName());
            statement.setDouble(4, item.getPrices());
            statement.setString(5, item.getInfo());
            statement.setString(6, userId);
            statement.setString(7, "WAITING");
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu san pham", e);
        }
    }

    private void initializeStorage() throws IOException, SQLException {
        ensureDataDirectoryExists();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_INVENTORY_TABLE_SQL);
        }
    }

    private void ensureDataDirectoryExists() throws IOException {
        if (Files.notExists(DATA_DIRECTORY)) {
            Files.createDirectories(DATA_DIRECTORY);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }
}
