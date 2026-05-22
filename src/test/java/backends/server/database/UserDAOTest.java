package backends.server.database;

import backends.common.models.accounts.User;
import backends.common.models.core.Account;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTest {
    private static final Path DB_PATH = Path.of("data", "users.db");
    private static final Path BACKUP_PATH = Path.of("data", "users.db.backup");
    private UserDAO userDAO;

    @BeforeEach
    void setUp() throws Exception {
        // Sao lưu DB thật trước khi test
        if (Files.exists(DB_PATH)) {
            Files.copy(DB_PATH, BACKUP_PATH, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(DB_PATH); // Xóa DB để dùng môi trường trống
        userDAO = new UserDAO();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Khôi phục lại DB thật
        if (Files.exists(BACKUP_PATH)) {
            Files.copy(BACKUP_PATH, DB_PATH, StandardCopyOption.REPLACE_EXISTING);
            Files.delete(BACKUP_PATH);
        }
    }

    @Test
    void testSaveAndGetUser() throws Exception {
        User user = new User("U1", "John Doe", "john@example.com", "123456789", "password123");
        userDAO.saveUser(user);

        User fetchedUser = userDAO.getUser("U1");
        assertNotNull(fetchedUser);
        assertEquals("John Doe", fetchedUser.getName());
        assertEquals("123456789", fetchedUser.getPhoneNumber());
    }

    @Test
    void testAuthenticate() throws Exception {
        User user = new User("U2", "Jane Doe", "jane@example.com", "987654321", "securePass");
        userDAO.saveUser(user);

        Optional<Account> authResult = userDAO.authenticate("987654321", "securePass");
        assertTrue(authResult.isPresent());
        assertEquals("User", authResult.get().getRole());

        // Sai password
        assertFalse(userDAO.authenticate("987654321", "wrongPass").isPresent());
    }

    @Test
    void testUpdateBalance() throws Exception {
        User user = new User("U3", "Alice", "alice@example.com", "111222333", "pass");
        userDAO.saveUser(user);

        userDAO.update_balance(150.5, "U3");
        assertEquals(150.5, userDAO.get_balance("U3"));
    }
}