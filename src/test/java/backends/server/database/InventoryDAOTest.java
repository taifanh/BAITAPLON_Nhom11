package backends.server.database;

import com.bidding_system.backends.common.models.core.Item;
import com.bidding_system.backends.common.models.items.Electronics;
import com.bidding_system.backends.server.database.InventoryDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("app.db")
public class InventoryDAOTest {
    private static final Path DB_PATH = Path.of("data", "app.db");
    private static final Path BACKUP_PATH = Path.of("data", "app.db.backup");
    private InventoryDAO inventoryDAO;

    @BeforeEach
    void setUp() throws Exception {
        if (Files.exists(DB_PATH)) {
            Files.copy(DB_PATH, BACKUP_PATH, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(DB_PATH);
        inventoryDAO = new InventoryDAO();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (Files.exists(BACKUP_PATH)) {
            Files.copy(BACKUP_PATH, DB_PATH, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(BACKUP_PATH);
        }
    }

    @Test
    void testSaveAndFindItem() throws Exception {
        Item item = new Electronics("ITEM1", "Laptop", 1000.0, "Gaming Laptop");
        item.setBidIncrement(50.0);
        inventoryDAO.saveItem(item, "USER1", "REQ1");

        Item fetchedItem = inventoryDAO.findById("ITEM1");
        assertNotNull(fetchedItem);
        assertEquals("Laptop", fetchedItem.getName());
        assertEquals("Electronics", fetchedItem.getType());
        assertEquals(1000.0, fetchedItem.getPrices());
    }

    @Test
    void testUpdateItemStatus() throws Exception {
        Item item = new Electronics("ITEM2", "Phone", 500.0, "Smartphone");
        inventoryDAO.saveItem(item, "USER1", "REQ2");

        inventoryDAO.updateItemStatus("ITEM2", InventoryDAO.STATUS_IN_PROGRESS);

        List<Item> inProgressItems = inventoryDAO.getItemsByStatus(InventoryDAO.STATUS_IN_PROGRESS);
        assertEquals(1, inProgressItems.size());
        assertEquals("ITEM2", inProgressItems.get(0).getId());
    }
}
