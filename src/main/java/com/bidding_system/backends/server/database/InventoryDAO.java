package com.bidding_system.backends.server.database;

import com.bidding_system.backends.common.constants.Statuses;
import com.bidding_system.backends.common.models.core.Item;
import com.bidding_system.backends.common.models.items.Art;
import com.bidding_system.backends.common.models.items.Electronics;
import com.bidding_system.backends.common.models.items.Vehicle;

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

public class InventoryDAO {
    // Trạng thái Item
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_SOLD = "SOLD";
    public static final String STATUS_UNSOLD = "UNSOLD";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path DATABASE_FILE = DATA_DIRECTORY.resolve("inventory.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;
    private static final String CREATE_INVENTORY_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS inventory (
                ItemId TEXT PRIMARY KEY,
                type TEXT NOT NULL,
                name TEXT NOT NULL,
                price DOUBLE,
                bidIncrement DOUBLE DEFAULT 0,
                itemDescription TEXT,
                request_id TEXT,
                 userId TEXT,
                status VARCHAR(20),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    public InventoryDAO() throws IOException {
        try {
            initializeStorage();
        } catch (SQLException e) {
            throw new IOException("Khong the khoi tao bang inventory", e);
        }
    }

    //Lưu sản phẩm vào kho dùng khi tạo accept request ở requestlog
    public synchronized void saveItem(Item item, String userId ,String request_id) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO inventory(ItemId,type,name,price,bidIncrement,itemDescription,request_id,userId,status)
                     VALUES(?,?,?,?,?,?,?,?,?)
                     """)) {
            statement.setString(1, item.getId());
            statement.setString(2, item.getType());
            statement.setString(3, item.getName());
            statement.setDouble(4, item.getPrices());
            statement.setDouble(5, item.getBidIncrement());
            statement.setString(6, item.getInfo());
            statement.setString(7, request_id);
            statement.setString(8, userId);
            statement.setString(9, Statuses.WAITING);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu san pham", e);
        }
    }

    //Tìm sản phẩm theo ID sản phẩm
    public Item findById(String itemId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT ItemId, type, name, price, bidIncrement, itemDescription
                     FROM inventory
                     WHERE ItemId = ?
                     """)) {
            statement.setString(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return getItem(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IOException("Khong the tim san pham theo id", e);
        }
    }

    //Lấy sản phẩm theo status
    public List<Item> getItemsByStatus(String status) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT ItemId, type, name, price, bidIncrement, itemDescription
                     FROM inventory
                     WHERE status = ?
                     """)) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                return getListItems(resultSet);
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay san pham theo trang thai", e);
        }
    }

    //thêm item vào phiên đấu giá theo TIMESTAMP và STATUS
    public Item getItemtoAuction(String status) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 SELECT ItemId, type, name, price, bidIncrement, itemDescription
                 FROM inventory
                 WHERE status = ?
                 ORDER BY created_at ASC
                 LIMIT 1
                 """)) {

            statement.setString(1, status);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return getItem(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay san pham theo trang thai", e);
        }
    }

    //Lấy sản phẩm theo Id user ( dùng để làm bảng riêng cho mỗi user )
    public List<Item> getItemsByUserId(String userId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT ItemId, type, name, price, bidIncrement, itemDescription
                     FROM inventory
                     WHERE userId = ?
                     """)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return getListItems(resultSet);
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay san pham theo user", e);
        }
    }

    public String getUserIdByItemId(String itemId) {
        String sql = "SELECT userId FROM inventory WHERE ItemId = ?";

        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("userId");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getStatusById(String request_id) {
        String sql = "SELECT status FROM inventory WHERE request_id = ?";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, request_id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("status");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Cập nhât status cho Item (Waiting -> InAuction -> Sold)
    public synchronized void updateItemStatus(List<String> itemIds, String status) throws IOException {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE inventory
                     SET status = ?
                     WHERE ItemId = ?
                     """)) {
            for (String itemId : itemIds) {
                statement.setString(1, status);
                statement.setString(2, itemId);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IOException("Khong the cap nhat trang thai danh sach san pham", e);
        }
    }

    public synchronized void updateItemStatus(String itemId, String status) throws IOException {
        if (itemId == null) {
            return;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE inventory
                     SET status = ?
                     WHERE ItemId = ?
                     """)) {
            statement.setString(1, status);
            statement.setString(2, itemId);
            statement.addBatch();
            statement.executeBatch();
        } catch (SQLException e) {
            throw new IOException("Khong the cap nhat trang thai danh sach san pham", e);
        }
    }
    // dùng cho chức năng remove item ở user
    public boolean item_exist(String requestId) throws IOException {
        try( Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
            SELECT EXISTS(
            SELECT 1
            FROM inventory
            WHERE request_id = ?)
            AS is_exists;
""")){
            statement.setString(1,requestId);
            try(ResultSet resultSet = statement.executeQuery()){
                return resultSet.next() && resultSet.getInt(1) ==1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public synchronized String getRequestIdbyItem(String itemId) throws IOException {
        try(Connection connection = openConnection();
        PreparedStatement statement = connection.prepareStatement(
                """
                 SELECT request_id FROM inventory
                 WHERE ItemId = ?
"""
        )){
            statement.setString(1,itemId);
            try(ResultSet resultSet = statement.executeQuery()){
                return resultSet.getString("request_id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public synchronized void removeItem(String requestId) throws IOException {
        try(Connection connection = openConnection();
            PreparedStatement statement = connection.prepareStatement("""
             DELETE FROM inventory
             WHERE request_id = ?
""")){
            statement.setString(1,requestId);
            if (item_exist(requestId)) {
                statement.addBatch();
                statement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private synchronized void initializeStorage() throws IOException, SQLException {
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

    //Chuyển SQL thành List<Item>
    private List<Item> getListItems(ResultSet resultSet) throws SQLException {
        List<Item> items = new ArrayList<>();
        while (resultSet.next()) {
            items.add(getItem(resultSet));
        }
        return items;
    }

    //Chuyển 1 dòng SQL thành Object Item
    private Item getItem(ResultSet resultSet) throws SQLException {
        String itemId = resultSet.getString("ItemId");
        String type = resultSet.getString("type");
        String name = resultSet.getString("name");
        double price = resultSet.getDouble("price");
        double bidIncrement = resultSet.getDouble("bidIncrement");
        String description = resultSet.getString("itemDescription");

        Item item = switch (type) {
            case "Electronics" -> new Electronics(itemId, name, price, description);
            case "Art" -> new Art(itemId, name, price, description);
            case "Vehicle" -> new Vehicle(itemId, name, price, description);
            default -> throw new SQLException("Loai san pham khong hop le: " + type);
        };
        item.setBidIncrement(bidIncrement);
        return item;
    }
}
