package Database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Auction_Item {
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("auctions_item.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_AUCTION_ITEMS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS auction_items (
                auctionId TEXT NOT NULL,
                itemId TEXT NOT NULL,
                PRIMARY KEY (auctionId, itemId),
                FOREIGN KEY (auctionId) REFERENCES auctions(auctionId),
                FOREIGN KEY (itemId) REFERENCES inventory(ItemId)
            )
            """;

    public Auction_Item() throws IOException {
        try {
            initializeStorage();
        } catch (SQLException e) {
            throw new IOException("Khong the khoi tao bang auction_items", e);
        }
    }

    public void saveAuctionItem(String auctionId, String itemId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO auction_items(auctionId,itemId)
                     VALUES(?,?)
                     """)) {
            statement.setString(1, auctionId);
            statement.setString(2, itemId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu auction item", e);
        }
    }

    private void initializeStorage() throws IOException, SQLException {
        ensureDataDirectoryExists();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_AUCTION_ITEMS_TABLE_SQL);
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
