package backends.server.database;

import backends.common.models.accounts.Admin;
import backends.common.models.accounts.User;
import backends.common.models.core.Account;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserDAO.
 *
 * Strategy (no reflection, no System.setProperty):
 *  - UserDAO always writes to  <cwd>/data/app.db  (Path.of("data") is relative to OS CWD).
 *  - On CI the directory does not exist yet; UserDAO.initializeStorage() creates it automatically.
 *  - @BeforeEach simply deletes the DB file so every test starts with a clean, empty schema.
 *  - @AfterAll deletes the DB file one final time to leave the workspace tidy.
 *  - No backup / restore is needed because CI runners start from a clean checkout.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ResourceLock("app.db")
public class UserDAOTest {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path DB_FILE  = DATA_DIR.resolve("app.db");

    private UserDAO userDAO;

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @BeforeEach
    void setUp() throws Exception {
        // Delete DB so each test gets a fresh, empty database.
        Files.deleteIfExists(DB_FILE);
        // UserDAO constructor calls initializeStorage() → creates data/ and the users table.
        userDAO = new UserDAO();
    }

    @AfterAll
    static void cleanUp() throws Exception {
        Files.deleteIfExists(DB_FILE);
    }

    // ------------------------------------------------------------------
    // saveUser / getUser
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void testSaveAndGetUser() throws Exception {
        User user = new User("U1", "John Doe", "john@example.com", "123456789", "password123");
        userDAO.saveUser(user);

        User fetched = userDAO.getUser("U1");

        assertNotNull(fetched);
        assertEquals("U1",              fetched.getId());
        assertEquals("John Doe",        fetched.getName());
        assertEquals("123456789",       fetched.getPhoneNumber());
        assertEquals("john@example.com",fetched.getEmail());
    }

    // ------------------------------------------------------------------
    // getAllUsers
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    void testGetAllUsers_returnsAllSavedUsers() throws Exception {
        userDAO.saveUser(new User("U1", "Alice", "alice@example.com", "111000001", "p1"));
        userDAO.saveUser(new User("U2", "Bob",   "bob@example.com",   "111000002", "p2"));

        List<User> users = userDAO.getAllUsers();

        assertEquals(2, users.size());
    }

    @Test
    @Order(3)
    void testGetAllUsers_emptyTable() throws Exception {
        List<User> users = userDAO.getAllUsers();
        assertTrue(users.isEmpty());
    }

    // ------------------------------------------------------------------
    // authenticate
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    void testAuthenticate_correctCredentials_returnsUserAccount() throws Exception {
        userDAO.saveUser(new User("U2", "Jane Doe", "jane@example.com", "987654321", "securePass"));

        Optional<Account> result = userDAO.authenticate("987654321", "securePass");

        assertTrue(result.isPresent());
        assertEquals("User", result.get().getRole());
        assertEquals("U2",   result.get().getId());
    }

    @Test
    @Order(5)
    void testAuthenticate_wrongPassword_returnsEmpty() throws Exception {
        userDAO.saveUser(new User("U2", "Jane Doe", "jane@example.com", "987654321", "securePass"));

        Optional<Account> result = userDAO.authenticate("987654321", "wrongPass");

        assertFalse(result.isPresent());
    }

    @Test
    @Order(6)
    void testAuthenticate_unknownPhone_returnsEmpty() throws Exception {
        Optional<Account> result = userDAO.authenticate("000000000", "any");
        assertFalse(result.isPresent());
    }

    @Test
    @Order(7)
    void testAuthenticate_adminRole() throws Exception {
        Admin admin = new Admin("A1", "Super Admin", "admin@example.com", "999888777", "adminPass");
        userDAO.saveAdmin(admin);

        Optional<Account> result = userDAO.authenticate("999888777", "adminPass");

        assertTrue(result.isPresent());
        assertEquals("Admin", result.get().getRole());
    }

    // ------------------------------------------------------------------
    // phoneNumberExists
    // ------------------------------------------------------------------

    @Test
    @Order(8)
    void testPhoneNumberExists_existing() throws Exception {
        userDAO.saveUser(new User("U3", "Alice", "alice@example.com", "111222333", "pass"));
        assertTrue(userDAO.phoneNumberExists("111222333"));
    }

    @Test
    @Order(9)
    void testPhoneNumberExists_notExisting() throws Exception {
        assertFalse(userDAO.phoneNumberExists("999999999"));
    }

    // ------------------------------------------------------------------
    // balance
    // ------------------------------------------------------------------

    @Test
    @Order(10)
    void testUpdateBalance_setsCorrectValue() throws Exception {
        userDAO.saveUser(new User("U3", "Alice", "alice@example.com", "111222333", "pass"));

        userDAO.updateBalance(150.5, "U3");

        assertEquals(150.5, userDAO.getBalance("U3"), 1e-9);
    }

    @Test
    @Order(11)
    void testUpdateBalance_accumulates() throws Exception {
        userDAO.saveUser(new User("U4", "Bob", "bob@example.com", "444555666", "pass"));

        userDAO.updateBalance(100.0, "U4");
        userDAO.updateBalance(50.0,  "U4");

        assertEquals(150.0, userDAO.getBalance("U4"), 1e-9);
    }

    @Test
    @Order(12)
    void testGetBalance_initialValueIsZero() throws Exception {
        userDAO.saveUser(new User("U5", "Zero", "zero@example.com", "000111222", "pass"));
        assertEquals(0.0, userDAO.getBalance("U5"), 1e-9);
    }

    // ------------------------------------------------------------------
    // getNameById
    // ------------------------------------------------------------------

    @Test
    @Order(13)
    void testGetNameById_found() throws Exception {
        userDAO.saveUser(new User("U5", "Charlie", "charlie@example.com", "777888999", "pass"));
        assertEquals("Charlie", userDAO.getNameById("U5"));
    }

    @Test
    @Order(14)
    void testGetNameById_notFound_returnsNull() throws Exception {
        assertNull(userDAO.getNameById("nonexistent"));
    }

    // ------------------------------------------------------------------
    // change_info
    // ------------------------------------------------------------------


    // ------------------------------------------------------------------
    // constraint / error cases
    // ------------------------------------------------------------------

    @Test
    @Order(15)
    void testSaveUser_duplicatePhone_throwsException() throws Exception {
        userDAO.saveUser(new User("U7", "First",  "first@example.com",  "555666777", "pass"));

        // UNIQUE constraint on phone_number must cause an exception
        assertThrows(Exception.class, () ->
                userDAO.saveUser(new User("U8", "Second", "second@example.com", "555666777", "pass"))
        );
    }

    @Test
    @Order(16)
    void testGetUser_notFound_throwsException() {
        assertThrows(Exception.class, () -> userDAO.getUser("doesNotExist"));
    }

    @Test
    @Order(17)
    void testGetBalance_notFound_throwsException() {
        assertThrows(Exception.class, () -> userDAO.getBalance("doesNotExist"));
    }
}
