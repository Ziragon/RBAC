package com.example.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final DateTimeFormatter DATETIME_FULL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {}

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FULL_FORMATTER);
    }

    public static String getCurrentDateTimeShort() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    public static boolean isBefore(String date1, String date2) {
        LocalDateTime dt1 = parse(date1);
        LocalDateTime dt2 = parse(date2);
        return dt1.isBefore(dt2);
    }

    public static boolean isAfter(String date1, String date2) {
        LocalDateTime dt1 = parse(date1);
        LocalDateTime dt2 = parse(date2);
        return dt1.isAfter(dt2);
    }

    public static boolean isFuture(String date) {
        return parse(date).isAfter(LocalDateTime.now());
    }

    public static boolean isPast(String date) {
        return parse(date).isBefore(LocalDateTime.now());
    }

    public static String addDays(String date, int days) {
        LocalDateTime dt = parse(date);
        return dt.plusDays(days).format(DATETIME_FORMATTER);
    }

    public static String addHours(String date, int hours) {
        LocalDateTime dt = parse(date);
        return dt.plusHours(hours).format(DATETIME_FORMATTER);
    }

    public static String formatRelativeTime(String date) {
        LocalDateTime dt = parse(date);
        LocalDateTime now = LocalDateTime.now();

        long minutes = ChronoUnit.MINUTES.between(now, dt);
        long absMinutes = Math.abs(minutes);

        String timeStr;

        if (absMinutes < 1) {
            return "just now";
        } else if (absMinutes < 60) {
            timeStr = absMinutes + " minute" + (absMinutes != 1 ? "s" : "");
        } else if (absMinutes < 1440) {
            long hours = absMinutes / 60;
            timeStr = hours + " hour" + (hours != 1 ? "s" : "");
        } else {
            long days = absMinutes / 1440;
            timeStr = days + " day" + (days != 1 ? "s" : "");
        }

        return minutes > 0 ? "in " + timeStr : timeStr + " ago";
    }

    public static long daysBetween(String date1, String date2) {
        LocalDateTime dt1 = parse(date1);
        LocalDateTime dt2 = parse(date2);
        return ChronoUnit.DAYS.between(dt1, dt2);
    }

    private static LocalDateTime parse(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("Date cannot be empty");
        }

        String trimmed = date.trim();

        try {
            // yyyy-MM-dd HH:mm:ss
            if (trimmed.length() > 16) {
                return LocalDateTime.parse(trimmed, DATETIME_FULL_FORMATTER);
            }
            // yyyy-MM-dd HH:mm
            if (trimmed.length() > 10) {
                return LocalDateTime.parse(trimmed, DATETIME_FORMATTER);
            }
            // yyyy-MM-dd
            return LocalDate.parse(trimmed, DATE_FORMATTER).atStartOfDay();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + date);
        }
    }
}