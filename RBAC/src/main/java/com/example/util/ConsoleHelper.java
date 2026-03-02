package com.example.util;

import java.util.Scanner;

// Вспомогательный класс с упрощающими методами
// Сделан, чтобы уменьшить кол-во кода в CommandRegistry
public class ConsoleHelper {

    // Запрос строки у пользователя
    public static String prompt(Scanner scanner, String message) {
        System.out.print(message + ": ");
        return scanner.nextLine().trim();
    }

    // Запрос подтверждения
    public static boolean confirm(Scanner scanner, String message) {
        System.out.print(message + " (yes/no): ");
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("yes") || input.equals("y") || input.equals("да");
    }

    // Запрос числа
    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message + " (" + min + "-" + max + "): ");
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Please enter a number between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    // Методы для более удобного вывода
    // Хедер
    public static void printHeader(String title) {
        System.out.println("\n=== " + title.toUpperCase() + " ===\n");
    }

    // Success
    public static void printSuccess(String message) {
        System.out.println("[OK] " + message);
    }

    // Ошибка
    public static void printError(String message) {
        System.out.println("[ERROR] " + message);
    }

    // Warning
    public static void printWarning(String message) {
        System.out.println("[WARNING] " + message);
    }

    // Info
    public static void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }

    // Разделитель
    public static void printSeparator() {
        System.out.println("-".repeat(60));
    }

    // Очистка экрана
    public static void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    // Меню выбора
    public static int showMenu(Scanner scanner, String title, String... options) {
        System.out.println("\n" + title + ":");
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i]);
        }
        return promptInt(scanner, "Select option", 1, options.length);
    }
}