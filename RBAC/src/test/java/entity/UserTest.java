package entity;

import com.example.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Entity Tests")
class UserTest {

    @Nested
    @DisplayName("Valid User Creation")
    class ValidUserCreation {

        @Test
        @DisplayName("Should create user with valid data")
        void shouldCreateUserWithValidData() {
            User user = new User("zhu_yuan", "Zhu Yuan", "email@example.com");

            assertAll(
                    () -> assertEquals("zhu_yuan", user.username()),
                    () -> assertEquals("Zhu Yuan", user.fullname()),
                    () -> assertEquals("email@example.com", user.email())
            );
        }

        @Test
        @DisplayName("Should format user correctly")
        void shouldFormatUserCorrectly() {
            User user = new User("alice", "Alice Thymefield", "alice@example.com");

            String formatted = user.format();

            assertNotNull(formatted);
            assertTrue(formatted.contains("alice"));
            assertTrue(formatted.contains("Alice Thymefield"));
        }
    }

    @Nested
    @DisplayName("Username Validation")
    class UsernameValidation {

        @Test
        @DisplayName("Should throw exception for null username")
        void shouldThrowExceptionForNullUsername() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new User(null, "Zhu Yuan", "email@example.com")
            );

            assertNotNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for short username")
        void shouldThrowExceptionForShortUsername() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new User("zu", "Zhu Yuan", "email@example.com")
            );

            assertNotNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for username with invalid characters")
        void shouldThrowExceptionForInvalidCharacters() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new User("юзер", "Zhu Yuan", "email@example.com")
            );

            assertNotNull(exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Email Validation")
    class EmailValidation {

        @Test
        @DisplayName("Should throw exception for email without domain")
        void shouldThrowExceptionForEmailWithoutDomain() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new User("zhu_yuan", "Zhu Yuan", "email@example")
            );

            assertNotNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should accept valid email formats")
        void shouldAcceptValidEmailFormats() {
            assertDoesNotThrow(() ->
                    new User("user1", "User One", "test@domain.com")
            );
            assertDoesNotThrow(() ->
                    new User("user2", "User Two", "test.user@sub.domain.org")
            );
        }
    }
}