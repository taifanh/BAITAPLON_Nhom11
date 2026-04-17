package Database;

import models.bidding.Auction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Auctions {
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("auctions.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_AUCTIONS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS auctions (
                auctionId TEXT PRIMARY KEY,
                startAt TEXT NOT NULL,
                endAt TEXT NOT NULL,
                status TEXT NOT NULL,
                highestBid DOUBLE DEFAULT 0
            )
            """;

    public Auctions() throws IOException {
        try {
            initializeStorage();
        } catch (SQLException e) {
            throw new IOException("Khong the khoi tao bang auctions", e);
        }
    }

    public void saveAuction(Auction auction) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO auctions(auctionId,startAt,endAt,status,highestBid)
                     VALUES(?,?,?,?,?)
                     """)) {
            statement.setString(1, auction.getAuctionId());
            statement.setString(2, auction.getStartAt().toString());
            statement.setString(3, auction.getEndAt().toString());
            //Date đọc bằng toString() lấy bằng parse()
            statement.setString(4, auction.getStatus().name());
            //Status đọc bằng name() lấy bằng valuesOf()
            statement.setDouble(5, auction.getCurrentHighestBid());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu auction", e);
        }
    }

    private void initializeStorage() throws IOException, SQLException {
        ensureDataDirectoryExists();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_AUCTIONS_TABLE_SQL);
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
