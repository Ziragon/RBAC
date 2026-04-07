package com.example.util;

import java.util.List;
import java.util.Scanner;

// Вспомогательный класс с упрощающими методами
// Сделан, чтобы уменьшить кол-во кода в CommandRegistry
public class ConsoleHelper {

    private ConsoleHelper() {}

    // Запрос строки у пользователя
    private static String prompt(Scanner scanner, String message) {
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
            } catch (NumberFormatException _) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    // Запрос Username (Валидация)
    public static String promptUsername(Scanner scanner, String message) {
        while (true) {
            String input = prompt(scanner, message);
            if (ValidationUtils.isValidUsername(input)) {
                return input;
            }
            printError("Invalid username. Use 3-20 characters: letters, digits, underscore.");
        }
    }

    // Запрос Email (Валидация)
    public static String promptEmail(Scanner scanner, String message) {
        while (true) {
            String input = prompt(scanner, message);
            if (ValidationUtils.isValidEmail(input)) {
                return input;
            }
            printError("Invalid email format.");
        }
    }

    // Запрос даты (Валидация)
    public static String promptDate(Scanner scanner, String message) {
        while (true) {
            String input = prompt(scanner, message + " (yyyy-MM-dd HH:mm)");
            if (ValidationUtils.isValidDate(input)) {
                return input;
            }
            printError("Invalid date format. Use: yyyy-MM-dd HH:mm");
        }
    }

    // Запрос даты (Дата в будущем)
    public static String promptFutureDate(Scanner scanner, String message) {
        while (true) {
            String input = prompt(scanner, message + " (yyyy-MM-dd HH:mm)");
            if (ValidationUtils.isFutureDate(input)) {
                return input;
            }
            printError("Date must be in the future. Format: yyyy-MM-dd HH:mm");
        }
    }

    // Не пустая строка
    private static String promptNonEmpty(Scanner scanner, String message) {
        while (true) {
            String input = prompt(scanner, message);
            if (!input.isEmpty()) {
                return input;
            }
            printError("Value cannot be empty.");
        }
    }

    // Запрос строки
    // required = true - запрашивает до ненулевой строки
    public static String promptString(Scanner scanner, String message, boolean required) {
        if (required) {
            return promptNonEmpty(scanner, message);
        }
        return prompt(scanner, message);
    }

    // Выбор элемента из списка
    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options list cannot be empty");
        }

        System.out.println("\n" + message + ":");
        for (int i = 0; i < options.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + options.get(i));
        }

        int choice = promptInt(scanner, "Select option", 1, options.size());
        return options.get(choice - 1);
    }

    // Методы для более удобного вывода
    // Хедер
    public static void printHeader(String title) {
        System.out.println(FormatUtils.formatHeader(title));
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