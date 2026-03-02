package com.example.util;

import java.util.List;

public class FormatUtils {

    private FormatUtils() {}

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }

        // Ширина каждого столбца
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, headers.length); i++) {
                String cell = row[i] != null ? row[i] : "";
                widths[i] = Math.max(widths[i], cell.length());
            }
        }

        // Отступы
        for (int i = 0; i < widths.length; i++) {
            widths[i] += 2;
        }

        StringBuilder sb = new StringBuilder();

        // Верхняя граница
        sb.append(buildBorder(widths)).append("\n");

        // Заголовки
        sb.append(buildRow(headers, widths)).append("\n");

        // Разделитель после заголовков
        sb.append(buildBorder(widths)).append("\n");

        // Строки данных
        for (String[] row : rows) {
            sb.append(buildRow(row, widths)).append("\n");
        }

        // Нижняя граница
        sb.append(buildBorder(widths)).append("\n");

        return sb.toString();
    }

    public static String formatBox(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String[] lines = text.split("\n");
        int maxLength = 0;
        for (String line : lines) {
            maxLength = Math.max(maxLength, line.length());
        }

        int boxWidth = maxLength + 4;

        StringBuilder sb = new StringBuilder();
        sb.append("+").append("-".repeat(boxWidth - 2)).append("+").append("\n");

        for (String line : lines) {
            sb.append("| ").append(padRight(line, maxLength)).append(" |").append("\n");
        }

        sb.append("+").append("-".repeat(boxWidth - 2)).append("+").append("\n");

        return sb.toString();
    }

    public static String formatHeader(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String upper = text.toUpperCase();
        int totalWidth = upper.length() + 8;

        return "\n" +
                "+" + "-".repeat(totalWidth - 2) + "+" + "\n" +
                "|   " + upper + "   |" + "\n" +
                "+" + "-".repeat(totalWidth - 2) + "+" + "\n";
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (maxLength < 4) return text.substring(0, Math.min(text.length(), maxLength));
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        return text + " ".repeat(length - text.length());
    }

    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text;
        return " ".repeat(length - text.length()) + text;
    }

    // Горизонтальная граница таблицы
    private static String buildBorder(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int width : widths) {
            sb.append("-".repeat(width)).append("+");
        }
        return sb.toString();
    }

    // Строка таблицы
    private static String buildRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String cell = (i < cells.length && cells[i] != null) ? cells[i] : "";
            sb.append(" ").append(padRight(cell, widths[i] - 2)).append(" |");
        }
        return sb.toString();
    }
}