package backends.server.database;

import com.bidding_system.backends.common.models.accounts.User;
import com.bidding_system.backends.common.models.core.Account;
import com.bidding_system.backends.server.database.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTest {

    @TempDir
    Path tempDir;

    private UserDAO userDAO;

    @BeforeEach
    void setUp() throws Exception {
        // Redirect UserDAO's DATA_DIRECTORY and DATABASE_FILE to tempDir via reflection
        // so each test runs against a fresh, isolated DB without touching the real data/
        setStaticFinalField(UserDAO.class, "DATA_DIRECTORY", tempDir);
        setStaticFinalField(UserDAO.class, "DATABASE_FILE", tempDir.resolve("users.db"));
        setStaticFinalField(UserDAO.class, "DATABASE_URL",
                "jdbc:sqlite:" + tempDir.resolve("users.db"));

        userDAO = new UserDAO();
    }

    @AfterEach
    void tearDown() {
        // TempDir is cleaned up automatically by JUnit 5; nothing extra needed
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Uses reflection to overwrite a private static final field.
     * Works for ordinary object fields; not needed for primitive-typed constants
     * that the compiler inlines (none here).
     */
    private static void setStaticFinalField(Class<?> clazz, String fieldName, Object value)
            throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        // Remove 'final' modifier so we can write to it
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

        field.set(null, value);
    }

    // ------------------------------------------------------------------ tests

    @Test
    void testSaveAndGetUser() throws Exception {
        User user = new User("U1", "John Doe", "john@example.com", "123456789", "password123");
        userDAO.saveUser(user);

        User fetched = userDAO.getUser("U1");

        assertNotNull(fetched);
        assertEquals("U1",         fetched.getId());
        assertEquals("John Doe",   fetched.getName());
        assertEquals("123456789",  fetched.getPhoneNumber());
        assertEquals("john@example.com", fetched.getEmail());
    }

    @Test
    void testGetAllUsers() throws Exception {
        userDAO.saveUser(new User("U1", "Alice", "alice@example.com", "111000001", "p1"));
        userDAO.saveUser(new User("U2", "Bob",   "bob@example.com",   "111000002", "p2"));

        List<User> users = userDAO.getAllUsers();

        assertEquals(2, users.size());
    }

    @Test
    void testAuthenticate_success() throws Exception {
        User user = new User("U2", "Jane Doe", "jane@example.com", "987654321", "securePass");
        userDAO.saveUser(user);

        Optional<Account> result = userDAO.authenticate("987654321", "securePass");

        assertTrue(result.isPresent());
        assertEquals("User", result.get().getRole());
        assertEquals("U2",   result.get().getId());
    }

    @Test
    void testAuthenticate_wrongPassword() throws Exception {
        userDAO.saveUser(new User("U2", "Jane Doe", "jane@example.com", "987654321", "securePass"));

        Optional<Account> result = userDAO.authenticate("987654321", "wrongPass");

        assertFalse(result.isPresent());
    }

    @Test
    void testAuthenticate_unknownPhone() throws Exception {
        Optional<Account> result = userDAO.authenticate("000000000", "any");

        assertFalse(result.isPresent());
    }

    @Test
    void testPhoneNumberExists() throws Exception {
        userDAO.saveUser(new User("U3", "Alice", "alice@example.com", "111222333", "pass"));

        assertTrue(userDAO.phoneNumberExists("111222333"));
        assertFalse(userDAO.phoneNumberExists("999999999"));
    }

    @Test
    void testUpdateAndGetBalance() throws Exception {
        userDAO.saveUser(new User("U3", "Alice", "alice@example.com", "111222333", "pass"));

        userDAO.update_balance(150.5, "U3");

        assertEquals(150.5, userDAO.get_balance("U3"), 1e-9);
    }

    @Test
    void testUpdateBalance_accumulates() throws Exception {
        userDAO.saveUser(new User("U4", "Bob", "bob@example.com", "444555666", "pass"));

        userDAO.update_balance(100.0, "U4");
        userDAO.update_balance(50.0,  "U4");

        assertEquals(150.0, userDAO.get_balance("U4"), 1e-9);
    }

    @Test
    void testGetNameById() throws Exception {
        userDAO.saveUser(new User("U5", "Charlie", "charlie@example.com", "777888999", "pass"));

        assertEquals("Charlie", userDAO.getNameById("U5"));
        assertNull(userDAO.getNameById("nonexistent"));
    }

    @Test
    void testChangeInfo() throws Exception {
        userDAO.saveUser(new User("U6", "Old Name", "old@example.com", "100200300", "oldPass"));

        userDAO.change_info("New Name", "new@example.com", "300200100", "newPass", "U6");

        // Authenticate with the new credentials to verify the update was persisted
        Optional<Account> result = userDAO.authenticate("300200100", "newPass");
        assertTrue(result.isPresent());
        assertEquals("New Name", result.get().getName());
    }

    @Test
    void testSaveUser_duplicatePhoneThrows() throws Exception {
        userDAO.saveUser(new User("U7", "First",  "first@example.com",  "555666777", "pass"));

        // phone_number has a UNIQUE constraint — second insert must fail
        assertThrows(Exception.class, () ->
                userDAO.saveUser(new User("U8", "Second", "second@example.com", "555666777", "pass"))
        );
    }

    @Test
    void testGetUser_notFound_throws() {
        assertThrows(Exception.class, () -> userDAO.getUser("doesNotExist"));
    }

    @Test
    void testGetBalance_notFound_throws() {
        assertThrows(Exception.class, () -> userDAO.get_balance("doesNotExist"));
    }
}