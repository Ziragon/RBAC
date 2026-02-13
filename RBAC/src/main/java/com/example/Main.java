package com.example;

import com.example.entity.User;

public class Main {
    static void main() {
        // дефолтное создание
        User user1 = new User("zhu_yuan", "Zhu Yuan", "email@example.com");
        System.out.println(user1.format());

        // null username
        try {
            new User(null, "Zhu Yuan", "email@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // короткий username
        try {
            new User("zu", "Zhu Yuan", "email@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // неразрешенные символы в username
        try {
            new User("юзер", "Zhu Yuan", "email@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // email без точки после @
        try {
            new User("zhu_yuan", "Zhu Yuan", "email@example");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
