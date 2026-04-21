package Database;

import models.core.Item;
import models.items.Art;
import models.items.Electronics;
import models.items.Vehicle;

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

public class Inventory {
    // Trạng thái Item
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_IN_AUCTION = "IN_AUCTION";
    public static final String STATUS_SOLD = "SOLD";

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

    //Lưu sản phẩm vào kho
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
            statement.setString(7, STATUS_WAITING);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Khong the luu san pham", e);
        }
    }

    //Tìm sản phẩm theo ID sản phẩm
    public Item findById(String itemId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT ItemId, type, name, price, itemDescription
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
                     SELECT ItemId, type, name, price, itemDescription
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

    public Item getItemByStatus(String status) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT ItemId, type, name, price, itemDescription
                     FROM inventory
                     WHERE status = ?
                     """)) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                return getItem(resultSet);
            }
        } catch (SQLException e) {
            throw new IOException("Khong the lay san pham theo trang thai", e);
        }
    }

    //Lấy sản phẩm theo Id user ( dùng để làm bảng riêng cho mỗi user )
    public List<Item> getItemsByUserId(String userId) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT ItemId, type, name, price, itemDescription
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

    // Cập nhât status cho Item (Waiting -> InAuction -> Sold)
    public void updateItemStatus(List<String> itemIds, String status) throws IOException {
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

    public void updateItemStatus(String itemId, String status) throws IOException {
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
        String description = resultSet.getString("itemDescription");

        return switch (type) {
            case "Electronics" -> new Electronics(itemId, name, price, description);
            case "Art" -> new Art(itemId, name, price, description);
            case "Vehicle" -> new Vehicle(itemId, name, price, description);
            default -> throw new SQLException("Loai san pham khong hop le: " + type);
        };
    }
}
