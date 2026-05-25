package com.bidding_system.backends.server.database;

import com.bidding_system.backends.common.messages.MsgBid.ServerBidRespond;
import com.bidding_system.backends.common.models.bidding.BidTransaction;

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

public class BidTransactionDAO {
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
                isAuto    BOOLEAN NOT NULL DEFAULT 0,
                maxBid    DOUBLE NOT NULL DEFAULT 0,
                bidTime TEXT NOT NULL
            )
            """;

    public BidTransactionDAO() throws IOException {
        try {
            initializeStorage();
        } catch (SQLException e) {
            throw new IOException("Khong the khoi tao bang bid_transactions", e);
        }
    }

    public synchronized void saveBid(String auctionId, BidTransaction bid) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO bid_transactions(auctionId, bidderId, itemId, amount, isAuto, maxBid, bidTime)
                 VALUES(?,?,?,?,?,?,?)
                 """)) {
            statement.setString(1, auctionId);
            statement.setString(2, bid.getBidderId());
            statement.setString(3, bid.item().getId());
            statement.setDouble(4, bid.getAmount());
            statement.setBoolean(5, bid.isAuto());
            statement.setDouble(6, bid.getMaxBid());
            statement.setString(7, bid.getTime().toInstant().toString()); // ← 7, không phải 5
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu lich su bid", e);
        }
    }

    public ServerBidRespond getMaxBidder(String auctionId) throws IOException, SQLException {
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT bidderId, amount, isAuto, maxBid
                    FROM bid_transactions
                    WHERE auctionId = ?
                    ORDER BY amount DESC 
                    LIMIT 1
                    """)) {
            statement.setString(1, auctionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                UserDAO userDAO = new UserDAO();
                String userId   = userDAO.getUser(rs.getString("bidderId")).getId();
                String username = userDAO.getUser(rs.getString("bidderId")).getName();
                ServerBidRespond respond = new ServerBidRespond(username, rs.getDouble("amount"), userId);
                respond.isAuto = rs.getBoolean("isAuto");
                respond.maxBid = rs.getDouble("maxBid");
                return respond;
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

    public List<BidHistoryDisplayRecord> getBidHistoryForDisplay(String auctionId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT auctionId, bidderId, itemId, amount, bidTime
                     FROM bid_transactions
                     WHERE auctionId = ?
                     ORDER BY bidTime ASC, id ASC
                     """)) {
            statement.setString(1, auctionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BidHistoryDisplayRecord> history = new ArrayList<>();
                UserDAO userDAO = new UserDAO();
                while (resultSet.next()) {
                    String bidderId = resultSet.getString("bidderId");
                    String bidderName = bidderId;
                    try {
                        bidderName = userDAO.getUser(bidderId).getName();
                    } catch (IOException ignored) {
                        // Fallback to bidder id if the user record is not available.
                    }
                    history.add(new BidHistoryDisplayRecord(
                            resultSet.getString("auctionId"),
                            bidderId,
                            bidderName,
                            resultSet.getString("itemId"),
                            resultSet.getDouble("amount"),
                            Instant.parse(resultSet.getString("bidTime"))
                    ));
                }
                return history;
            }
        } catch (SQLException e) {
            throw new IOException("Khong the doc lich su bid de hien thi", e);
        }
    }

    public List<BidHistoryRecord> getBidHistoryByBidder(String bidderId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT auctionId, bidderId, itemId, amount, bidTime
                     FROM bid_transactions
                     WHERE bidderId = ?
                     ORDER BY bidTime DESC, id DESC
                     """)) {
            statement.setString(1, bidderId);
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
            throw new IOException("Khong the doc lich su bid cua user", e);
        }
    }

    private synchronized void initializeStorage() throws IOException, SQLException {
        ensureDataDirectoryExists();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_BID_TRANSACTIONS_TABLE_SQL);
            ensureColumnExists(connection, "isAuto", "BOOLEAN NOT NULL DEFAULT 0");
            ensureColumnExists(connection, "maxBid",  "DOUBLE NOT NULL DEFAULT 0");
        }
    }

    private void ensureColumnExists(Connection conn, String column, String definition) {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "bid_transactions", column)) {
            if (!rs.next()) {
                conn.createStatement().executeUpdate(
                        "ALTER TABLE bid_transactions ADD COLUMN " + column + " " + definition);
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

    public record BidHistoryDisplayRecord(
            String auctionId,
            String bidderId,
            String bidderName,
            String itemId,
            double amount,
            Instant bidTime
    ) {}
}
