package util;

import com.example.util.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DateUtils Tests")
class DateUtilsTest {

    @Test
    @DisplayName("Should return current date and time")
    void shouldReturnCurrentDateTime() {
        String date = DateUtils.getCurrentDate();
        String dateTime = DateUtils.getCurrentDateTime();

        assertAll(
                () -> assertNotNull(date),
                () -> assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}")),
                () -> assertNotNull(dateTime),
                () -> assertTrue(dateTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))
        );
    }

    @Test
    @DisplayName("Should compare dates correctly")
    void shouldCompareDates() {
        assertAll(
                () -> assertTrue(DateUtils.isBefore("2024-01-01 00:00", "2025-01-01 00:00")),
                () -> assertFalse(DateUtils.isBefore("2025-01-01 00:00", "2024-01-01 00:00")),
                () -> assertTrue(DateUtils.isAfter("2025-01-01 00:00", "2024-01-01 00:00")),
                () -> assertTrue(DateUtils.isPast("2020-01-01 00:00")),
                () -> assertTrue(DateUtils.isFuture("2030-01-01 00:00"))
        );
    }

    @Test
    @DisplayName("Should add days to date")
    void shouldAddDays() {
        String result = DateUtils.addDays("2024-01-01 12:00", 10);

        assertEquals("2024-01-11 12:00", result);
    }

    @Test
    @DisplayName("Should format relative time")
    void shouldFormatRelativeTime() {
        String pastDate = DateUtils.addDays(DateUtils.getCurrentDateTimeShort(), -3);
        String futureDate = DateUtils.addDays(DateUtils.getCurrentDateTimeShort(), 5);

        String pastRelative = DateUtils.formatRelativeTime(pastDate);
        String futureRelative = DateUtils.formatRelativeTime(futureDate);

        assertAll(
                () -> assertTrue(pastRelative.contains("ago")),
                () -> assertTrue(futureRelative.contains("in "))
        );
    }

    @Test
    @DisplayName("Should calculate days between dates")
    void shouldCalculateDaysBetween() {
        long days = DateUtils.daysBetween("2024-01-01 00:00", "2024-01-11 00:00");

        assertEquals(10, days);
    }

    @Test
    @DisplayName("Should handle different date formats")
    void shouldHandleDifferentFormats() {
        assertAll(
                () -> assertDoesNotThrow(() -> DateUtils.isFuture("2030-01-01")),
                () -> assertDoesNotThrow(() -> DateUtils.isFuture("2030-01-01 12:00")),
                () -> assertDoesNotThrow(() -> DateUtils.isFuture("2030-01-01 12:00:00")),
                () -> assertThrows(IllegalArgumentException.class, () -> DateUtils.isFuture("invalid")),
                () -> assertThrows(IllegalArgumentException.class, () -> DateUtils.isFuture(null))
        );
    }
}