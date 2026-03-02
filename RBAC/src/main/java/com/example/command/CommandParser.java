package com.example.command;

import com.example.system.RBACSystem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {

    private final Map<String, Command> commands;
    private final Map<String, String> commandDescriptions;

    public CommandParser() {
        this.commands = new LinkedHashMap<>();
        this.commandDescriptions = new LinkedHashMap<>();
    }

    public void registerCommand(String name, String description, Command command) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Command name cannot be null or empty");
        }
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        String normalizedName = name.toLowerCase().trim();
        commands.put(normalizedName, command);
        commandDescriptions.put(normalizedName, description != null ? description : "No description");
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        if (commandName == null || commandName.isBlank()) {
            System.out.println("Error: Empty command. Type 'help' for available commands.");
            return;
        }

        String normalizedName = commandName.toLowerCase().trim();
        Command command = commands.get(normalizedName);

        if (command == null) {
            System.out.println("Error: Unknown command '" + commandName + "'");
            System.out.println("Type 'help' for available commands.");
            return;
        }

        try {
            command.execute(scanner, system);
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.isBlank()) {
            return;
        }

        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0];

        executeCommand(commandName, scanner, system);
    }

    public void printHelp() {
        System.out.println("\n--- AVAILABLE COMMANDS ---\n");

        if (commands.isEmpty()) {
            System.out.println("No commands registered.");
        } else {
            for (Map.Entry<String, String> entry : commandDescriptions.entrySet()) {
                System.out.printf("  %-15s - %s%n", entry.getKey(), entry.getValue());
            }
        }

        System.out.println();
    }

    public boolean hasCommand(String name) {
        if (name == null) return false;
        return commands.containsKey(name.toLowerCase().trim());
    }

    public int getCommandCount() {
        return commands.size();
    }
}