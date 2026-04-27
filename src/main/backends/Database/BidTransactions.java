package Database;

import models.Extra.messages.ServerBidRespond;
import models.accounts.User;
import models.bidding.BidTransaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BidTransactions {
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("auctions.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_BID_TRANSACTIONS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS bid_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                auctionId TEXT NOT NULL,
                bidderId TEXT NOT NULL,
                itemId TEXT NOT NULL,
                amount DOUBLE NOT NULL,
                bidTime TEXT NOT NULL
            )
            """;

    public BidTransactions() throws IOException {
        try {
            initializeStorage();
        } catch (SQLException e) {
            throw new IOException("Khong the khoi tao bang bid_transactions", e);
        }
    }

    public void saveBid(String auctionId, BidTransaction bid) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO bid_transactions(auctionId, bidderId, itemId, amount, bidTime)
                     VALUES(?,?,?,?,?)
                     """)) {
            statement.setString(1, auctionId);
            statement.setString(2, bid.getBidderId());
            statement.setString(3, bid.item().getId());
            statement.setDouble(4, bid.getAmount());
            statement.setString(5, bid.getTime().toInstant().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu lich su bid", e);
        }
    }

    public ServerBidRespond getMaxBidder(String auctionId) throws IOException, SQLException {
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT bidderId, amount
                    FROM bid_transactions
                    WHERE auctionId = ?
                    ORDER BY amount DESC 
                    LIMIT 1
                    """)) {
            statement.setString(1, auctionId);
            try(ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                String username = (new UserStore()).getUser(resultSet.getString("bidderId")).getName();
                return new ServerBidRespond(username, resultSet.getDouble("amount"));
            } catch (SQLException e) {
                throw new IOException("Chua co bidder", e);
            }
        }
    }

    public List<BidHistoryRecord> getBidHistory(String auctionId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT auctionId, bidderId, itemId, amount, bidTime
                     FROM bid_transactions
                     WHERE auctionId = ?
                     ORDER BY bidTime ASC, id ASC
                     """)) {
            statement.setString(1, auctionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BidHistoryRecord> history = new ArrayList<>();
                while (resultSet.next()) {
                    history.add(new BidHistoryRecord(
                            resultSet.getString("auctionId"),
                            resultSet.getString("bidderId"),
                            resultSet.getString("itemId"),
                            resultSet.getDouble("amount"),
                            Instant.parse(resultSet.getString("bidTime"))
                    ));
                }
                return history;
            }
        } catch (SQLException e) {
            throw new IOException("Khong the doc lich su bid", e);
        }
    }

    private void initializeStorage() throws IOException, SQLException {
        ensureDataDirectoryExists();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_BID_TRANSACTIONS_TABLE_SQL);
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

    public record BidHistoryRecord(
            String auctionId,
            String bidderId,
            String itemId,
            double amount,
            Instant bidTime
    ) {}
}
