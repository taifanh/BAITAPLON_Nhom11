package Database;

import models.bidding.Auction;

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
                ItemId TEXT NOT NULL,
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
                     INSERT INTO auctions(auctionId,startAt,endAt,status,ItemId,highestBid)
                     VALUES(?,?,?,?,?,?)
                     """)) {
            statement.setString(1, auction.getAuctionId());
            statement.setString(2, auction.getStartAt().toString());
            statement.setString(3, auction.getEndAt().toString());
            //Date đọc bằng toString() lấy bằng parse()
            statement.setString(4, auction.getStatus().name());
            //Status đọc bằng name() lấy bằng valuesOf()
            statement.setString(5,auction.getItem().getId());
            statement.setDouble(6, auction.getCurrentHighestBid());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu auction", e);
        }
    }
    //Cập nhật bid cao nhất
    public void updateHighestBid(String auctionId, double highestBid) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE auctions
                     SET highestBid = ?
                     WHERE auctionId = ?
                     """)) {
            statement.setDouble(1, highestBid);
            statement.setString(2, auctionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the cap nhat highest bid", e);
        }
    }
    //Cập nhật trạng thái phiên đấu giá
    public void updateAuctionState(String auctionId, Auction.Status status, LocalDateTime endAt, double highestBid) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE auctions
                     SET status = ?, endAt = ?, highestBid = ?
                     WHERE auctionId = ?
                     """)) {
            statement.setString(1, status.name());
            statement.setString(2, endAt.toString());
            statement.setDouble(3, highestBid);
            statement.setString(4, auctionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the cap nhat trang thai auction", e);
        }
    }
    //Kiểm tra phiên đấu giá đang ACTIVE
    public boolean existsActiveAuctionForItem(String itemId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT 1
                     FROM auctions
                     WHERE ItemId = ? AND status = ?
                     LIMIT 1
                     """)) {
            statement.setString(1, itemId);
            statement.setString(2, Auction.Status.ACTIVE.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new IOException("Khong the kiem tra auction dang hoat dong", e);
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
