package util;

import com.example.util.ValidationUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationUtils Tests")
class ValidationUtilsTest {

    @Test
    @DisplayName("Should validate username")
    void shouldValidateUsername() {
        assertAll(
                () -> assertTrue(ValidationUtils.isValidUsername("Jane_doe")),
                () -> assertTrue(ValidationUtils.isValidUsername("user123")),
                () -> assertFalse(ValidationUtils.isValidUsername("ab")),
                () -> assertFalse(ValidationUtils.isValidUsername("user name")),
                () -> assertFalse(ValidationUtils.isValidUsername("user@name")),
                () -> assertFalse(ValidationUtils.isValidUsername(null))
        );
    }

    @Test
    @DisplayName("Should validate email")
    void shouldValidateEmail() {
        assertAll(
                () -> assertTrue(ValidationUtils.isValidEmail("test@example.com")),
                () -> assertTrue(ValidationUtils.isValidEmail("user.name@domain.org")),
                () -> assertFalse(ValidationUtils.isValidEmail("invalid")),
                () -> assertFalse(ValidationUtils.isValidEmail("test@")),
                () -> assertFalse(ValidationUtils.isValidEmail("@example.com")),
                () -> assertFalse(ValidationUtils.isValidEmail(null))
        );
    }

    @Test
    @DisplayName("Should validate date format")
    void shouldValidateDateFormat() {
        assertAll(
                () -> assertTrue(ValidationUtils.isValidDate("2026-12-31 23:59")),
                () -> assertTrue(ValidationUtils.isValidDate("2026-01-01 00:00")),
                () -> assertFalse(ValidationUtils.isValidDate("2026/12/31 23:59")),
                () -> assertFalse(ValidationUtils.isValidDate("31-12-2026 23:59")),
                () -> assertFalse(ValidationUtils.isValidDate("invalid")),
                () -> assertFalse(ValidationUtils.isValidDate(null))
        );
    }

    @Test
    @DisplayName("Should check future date")
    void shouldCheckFutureDate() {
        String futureDate = LocalDateTime.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String pastDate = LocalDateTime.now().minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        assertAll(
                () -> assertTrue(ValidationUtils.isFutureDate(futureDate)),
                () -> assertFalse(ValidationUtils.isFutureDate(pastDate))
        );
    }

    @Test
    @DisplayName("Should normalize strings")
    void shouldNormalizeStrings() {
        assertAll(
                () -> assertEquals("hello world", ValidationUtils.normalizeString("  hello   world  ")),
                () -> assertEquals("", ValidationUtils.normalizeString(null))
        );
    }

    @Test
    @DisplayName("Should throw for empty values")
    void shouldThrowForEmptyValues() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ValidationUtils.requireNonEmpty(null, "Field")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ValidationUtils.requireNonEmpty("", "Field")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ValidationUtils.requireNonEmpty("   ", "Field")),
                () -> assertDoesNotThrow(
                        () -> ValidationUtils.requireNonEmpty("value", "Field"))
        );
    }
}