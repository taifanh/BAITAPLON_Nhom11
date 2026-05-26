package backends.server.database;

import com.bidding_system.backends.common.messages.Common.Message;
import com.bidding_system.backends.common.messages.Common.MessageType;
import com.bidding_system.backends.server.database.MyRequestDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("app.db")
public class MyRequestDAOTest {
    private static final Path DB_PATH = Path.of("data", "app.db");
    private static final Path BACKUP_PATH = Path.of("data", "app.db.backup");
    private MyRequestDAO myRequestDAO;

    @BeforeEach
    void setUp() throws Exception {
        if (Files.exists(DB_PATH)) {
            Files.copy(DB_PATH, BACKUP_PATH, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(DB_PATH);
        Files.createDirectories(DB_PATH.getParent()); // ← thêm dòng này
        myRequestDAO = new MyRequestDAO();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (Files.exists(BACKUP_PATH)) {
            Files.copy(BACKUP_PATH, DB_PATH, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(BACKUP_PATH);
        }
    }

    @Test
    void testSaveAndFindRequest() throws Exception {
        Message msg = new Message();
        msg.Id_user = "USER1";
        msg.messageType = MessageType.ADD_ITEM.getValue();
        msg.payloadJson = "{\"name\": \"test item\"}";

        MyRequestDAO.save_myrequest(msg, "REQ123");

        MyRequestDAO.RequestRecord record = myRequestDAO.findByRequestId("REQ123");
        assertNotNull(record);
        assertEquals("USER1", record.userId());
        assertEquals(MessageType.ADD_ITEM.getValue(), record.requestType());
        assertEquals(MyRequestDAO.STATUS_PENDING, record.status());
    }

    @Test
    void testUpdateRequestStatus() throws Exception {
        Message msg = new Message();
        msg.Id_user = "USER2";
        msg.messageType = "withdraw"; // Note: "withdraw" type không được định nghĩa trong enum, sử dụng cho test
        MyRequestDAO.save_myrequest(msg, "REQ999");

        myRequestDAO.updateRequestStatus("REQ999", MyRequestDAO.STATUS_ACCEPTED);

        assertEquals(MyRequestDAO.STATUS_ACCEPTED, myRequestDAO.getStatusById("REQ999"));
    }
}
