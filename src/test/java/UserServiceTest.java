import org.junit.jupiter.api.*;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.UserService;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    static UserService userService;
    static int testUserId;

    @BeforeAll
    static void setup() {
        userService = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        // Nettoyer la base après chaque test
        List<User> users = userService.getAll();
        for (User user : users) {
            if (user.getUsername().startsWith("test_")) {
                userService.delete(user.getId());
            }
        }
    }

    @Test
    @Order(1)
    void testInsertUser() throws SQLException {
        User user = new User("test_user", "test@test.com", "password123", "Test User");
        userService.insert(user);

        assertTrue(user.getId() > 0);

        User saved = userService.getById(user.getId());
        assertNotNull(saved);
        assertEquals("test_user", saved.getUsername());
    }

    @Test
    @Order(2)
    void testGetAllUsers() throws SQLException {
        List<User> users = userService.getAll();
        assertNotNull(users);
        assertTrue(users.size() >= 0);
    }

    @Test
    @Order(3)
    void testUpdateUser() throws SQLException {
        // Créer un utilisateur de test
        User user = new User("test_update", "update@test.com", "pass", "Original Name");
        userService.insert(user);

        // Modifier
        user.setFullName("Modified Name");
        user.setRole("ADMIN");
        userService.update(user);

        // Vérifier
        User updated = userService.getById(user.getId());
        assertEquals("Modified Name", updated.getFullName());
        assertEquals("ADMIN", updated.getRole());
    }

    @Test
    @Order(4)
    void testDeleteUser() throws SQLException {
        User user = new User("test_delete", "delete@test.com", "pass", "To Delete");
        userService.insert(user);
        int id = user.getId();

        userService.delete(id);

        User deleted = userService.getById(id);
        assertNull(deleted);
    }

    @Test
    @Order(5)
    void testLoginSuccess() throws SQLException {
        User user = new User("test_login", "login@test.com", "correctpass", "Login Test");
        userService.insert(user);

        User loggedIn = userService.login("test_login", "correctpass");
        assertNotNull(loggedIn);
        assertEquals("test_login", loggedIn.getUsername());
    }

    @Test
    @Order(6)
    void testLoginFailure() throws SQLException {
        User loggedIn = userService.login("nonexistent", "wrongpass");
        assertNull(loggedIn);
    }

    @Test
    @Order(7)
    void testIsUsernameTaken() throws SQLException {
        User user = new User("unique_user", "unique@test.com", "pass", "Unique");
        userService.insert(user);

        assertTrue(userService.isUsernameTaken("unique_user"));
        assertFalse(userService.isUsernameTaken("not_taken"));
    }

    @Test
    @Order(8)
    void testIsEmailTaken() throws SQLException {
        User user = new User("email_test", "uniqueemail@test.com", "pass", "Email Test");
        userService.insert(user);

        assertTrue(userService.isEmailTaken("uniqueemail@test.com"));
        assertFalse(userService.isEmailTaken("notexist@test.com"));
    }
}