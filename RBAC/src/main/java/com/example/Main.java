package com.example;

import com.example.command.CommandParser;
import com.example.command.CommandRegistry;
import com.example.entity.Role;
import com.example.system.RBACSystem;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Role.clearNameRegistry();

        RBACSystem system = new RBACSystem();
        system.initialize();

        CommandParser parser = new CommandParser();
        CommandRegistry registry = new CommandRegistry(parser);
        registry.registerAll();

        Scanner scanner = new Scanner(System.in);

        System.out.println("\n+---------------------------------------+");
        System.out.println("|     RBAC Management System             |");
        System.out.println("|     Type 'help' for commands           |");
        System.out.println("+----------------------------------------+");

        while (true) {
            System.out.print("\n[" + system.getCurrentUser() + "]> ");
            String input = scanner.nextLine();
            parser.parseAndExecute(input, scanner, system);
        }
    }
}