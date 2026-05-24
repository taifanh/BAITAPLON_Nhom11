package backends.server.database;

import com.bidding_system.backends.common.models.bidding.Auction;
import com.bidding_system.backends.common.models.core.Item;
import com.bidding_system.backends.common.models.items.Art;
import com.bidding_system.backends.server.database.AuctionDAO;
import com.bidding_system.backends.server.database.InventoryDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionDAOTest {
    private static final Path AUCTION_DB = Path.of("data", "auctions.db");
    private static final Path AUCTION_BACKUP = Path.of("data", "auctions.db.backup");
    private static final Path INV_DB = Path.of("data", "inventoryDAO.db");
    private static final Path INV_BACKUP = Path.of("data", "inventoryDAO.db.backup");

    private AuctionDAO auctionDAO;
    private InventoryDAO inventoryDAO;

    @BeforeEach
    void setUp() throws Exception {
        backupAndClear(AUCTION_DB, AUCTION_BACKUP);
        backupAndClear(INV_DB, INV_BACKUP);
        inventoryDAO = new InventoryDAO();
        auctionDAO = new AuctionDAO();
    }

    @AfterEach
    void tearDown() throws Exception {
        restoreDb(AUCTION_DB, AUCTION_BACKUP);
        restoreDb(INV_DB, INV_BACKUP);
    }

    private void backupAndClear(Path db, Path backup) throws Exception {
        if (Files.exists(db)) {
            Files.copy(db, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(db);
    }

    private void restoreDb(Path db, Path backup) throws Exception {
        if (Files.exists(backup)) {
            Files.copy(backup, db, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(backup);
        }
    }

    @Test
    void testSaveAndGetActiveAuction() throws Exception {
        // Tạo trước item trong InventoryDAO
        Item item = new Art("A1", "Mona Lisa", 5000.0, "Classic painting");
        inventoryDAO.saveItem(item, "USER1", "REQ1");

        // Tạo Auction
        Auction auction = new Auction(item);
        auction.schedule(LocalDateTime.now(), java.time.Duration.ofHours(1));
        auction.start(LocalDateTime.now());

        auctionDAO.saveAuction(auction);

        List<Auction> activeAuctions = auctionDAO.getActiveAuctions();
        assertEquals(1, activeAuctions.size());
        assertEquals("A1", activeAuctions.get(0).getItem().getId());
    }
}