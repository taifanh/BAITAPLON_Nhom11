package backends.server.database;

import backends.common.models.bidding.Auction;
import backends.common.models.core.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("app.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String TABLE_NAME = "auction";
    private static final String CREATE_AUCTIONS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS auction (
                auctionId TEXT PRIMARY KEY,
                startAt TEXT NOT NULL,
                endAt TEXT NOT NULL,
                status TEXT NOT NULL,
                ItemId TEXT NOT NULL,
                highestBid DOUBLE DEFAULT 0,
                highestBidderId TEXT
            )
            """;

    public AuctionDAO() throws IOException {
        try {
            initializeStorage();
        } catch (SQLException e) {
            throw new IOException("Khong the khoi tao bang auction", e);
        }
    }

    public synchronized void saveAuction(Auction auction) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO auction(auctionId,startAt,endAt,status,ItemId,highestBid,highestBidderId)
                     VALUES(?,?,?,?,?,?,?)
                     """)) {
            statement.setString(1, auction.getAuctionId());
            statement.setString(2, auction.getStartAt().toString());
            statement.setString(3, auction.getEndAt().toString());
            statement.setString(4, auction.getStatus().name());
            statement.setString(5, auction.getItem().getId());
            statement.setDouble(6, auction.getCurrentHighestBid());
            statement.setString(7, auction.getCurrentHighestBidderId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu auction", e);
        }
    }

    public synchronized void updateHighestBid(String auctionId, double highestBid, String highestBidderId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE auction
                     SET highestBid = ?, highestBidderId = ?
                     WHERE auctionId = ?
                     """)) {
            statement.setDouble(1, highestBid);
            statement.setString(2, highestBidderId);
            statement.setString(3, auctionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the cap nhat highest bid", e);
        }
    }

    public synchronized void updateAuctionState(String auctionId, Auction.Status status, LocalDateTime endAt, double highestBid, String highestBidderId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE auction
                     SET status = ?, endAt = ?, highestBid = ?, highestBidderId = ?
                     WHERE auctionId = ?
                     """)) {
            statement.setString(1, status.name());
            statement.setString(2, endAt.toString());
            statement.setDouble(3, highestBid);
            statement.setString(4, highestBidderId);
            statement.setString(5, auctionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the cap nhat trang thai auction", e);
        }
    }

    public List<Auction> getActiveAuctions() throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT auctionId, startAt, endAt, status, ItemId, highestBid, highestBidderId
                     FROM auction
                     WHERE status = ?
                     ORDER BY endAt ASC
                     """)) {
            statement.setString(1, Auction.Status.ACTIVE.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Auction> auctions = new ArrayList<>();
                InventoryDAO inventoryDAO = new InventoryDAO();
                while (resultSet.next()) {
                    auctions.add(mapAuction(resultSet, inventoryDAO));
                }
                return auctions;
            }
        } catch (SQLException e) {
            throw new IOException("Khong the doc danh sach auction dang hoat dong", e);
        }
    }

    // cập nhật đếm giờ mới ===>> dùng cho anti sniping
    public synchronized void updateEndTime(String auctionId, LocalDateTime endAt) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 UPDATE auction
                 SET endAt = ?
                 WHERE auctionId = ?
                 """)) {
            statement.setString(1, endAt.toString());
            statement.setString(2, auctionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the cap nhat end time", e);
        }
    }

    private synchronized void initializeStorage() throws IOException, SQLException {
        ensureDataDirectoryExists();
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_AUCTIONS_TABLE_SQL);
            ensureColumnExists(connection, "highestBidderId", "TEXT");
        }
    }

    private void ensureColumnExists(Connection connection, String columnName, String columnDefinition) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(" + TABLE_NAME + ")");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnName + " " + columnDefinition);
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

    //Chuyển SQL về thành object auction

    private Auction mapAuction(ResultSet resultSet, InventoryDAO inventoryDAO) throws SQLException, IOException {
        String itemId = resultSet.getString("ItemId");
        Item item = inventoryDAO.findById(itemId);
        if (item == null) {
            throw new IOException("Khong tim thay item cua auction: " + itemId);
        }

        return Auction.restore(
                resultSet.getString("auctionId"),
                item,
                Auction.Status.valueOf(resultSet.getString("status")),
                LocalDateTime.parse(resultSet.getString("startAt")),
                LocalDateTime.parse(resultSet.getString("endAt")),
                resultSet.getDouble("highestBid"),
                resultSet.getString("highestBidderId")
        );
    }
}
