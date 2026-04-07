package com.example;

import com.example.command.CommandParser;
import com.example.command.CommandRegistry;
import com.example.entity.Role;
import com.example.system.RBACSystem;

import java.util.Scanner;

public class Main {
    @SuppressWarnings("unused")
    static void main(String[] args) {
        RBACSystem system = new RBACSystem();
        system.initialize();

        CommandParser parser = new CommandParser();
        CommandRegistry registry = new CommandRegistry(parser);
        registry.registerAll();

        System.out.println("\n+---------------------------------------+");
        System.out.println("|     RBAC Management System             |");
        System.out.println("|     Type 'help' for commands           |");
        System.out.println("+----------------------------------------+");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n[" + system.getCurrentUser() + "]> ");

                if (!scanner.hasNextLine()) {
                    System.out.println("Input stream closed. Exiting...");
                    break;
                }

                String input = scanner.nextLine();
                parser.parseAndExecute(input, scanner, system);
            }
        }
    }
}