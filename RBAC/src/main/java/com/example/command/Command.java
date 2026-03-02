package com.example.command;

import com.example.system.RBACSystem;

import java.util.Scanner;

@FunctionalInterface
public interface Command {

    void execute(Scanner scanner, RBACSystem system);
}