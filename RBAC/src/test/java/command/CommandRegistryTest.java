package command;

import com.example.command.CommandParser;
import com.example.command.CommandRegistry;
import com.example.entity.Role;
import com.example.system.RBACSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandRegistry Tests")
class CommandRegistryTest {

    private CommandParser parser;
    private RBACSystem system;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        Role.clearNameRegistry();
        parser = new CommandParser();
        system = new RBACSystem();
        system.initialize();

        // Перехватываем вывод в консоль
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        // Регистрируем команды
        CommandRegistry registry = new CommandRegistry(parser);
        registry.registerAll();
    }

    // Сканнер с заданным вводом
    private Scanner scannerWithInput(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes()));
    }

    // Вывод консоли
    private String getOutput() {
        return outputStream.toString();
    }

    @Test
    @DisplayName("Should register all commands")
    void shouldRegisterAllCommands() {
        assertTrue(parser.getCommandCount() > 20);
        assertTrue(parser.hasCommand("user-list"));
        assertTrue(parser.hasCommand("role-list"));
        assertTrue(parser.hasCommand("assign-role"));
        assertTrue(parser.hasCommand("help"));
        assertTrue(parser.hasCommand("stats"));
    }

    @Test
    @DisplayName("Should execute user-list command")
    void shouldExecuteUserList() {
        parser.executeCommand("user-list", scannerWithInput(""), system);

        String output = getOutput();
        assertAll(
                () -> assertTrue(output.contains("USER")),
                () -> assertTrue(output.contains("admin"))
        );
    }

    @Test
    @DisplayName("Should execute role-list command")
    void shouldExecuteRoleList() {
        parser.executeCommand("role-list", scannerWithInput(""), system);

        String output = getOutput();
        assertAll(
                () -> assertTrue(output.contains("Administrator")),
                () -> assertTrue(output.contains("Manager")),
                () -> assertTrue(output.contains("Viewer"))
        );
    }

    @Test
    @DisplayName("Should create user with valid input")
    void shouldCreateUser() {
        String input = "testuser\nTest User\ntest@example.com\n";

        parser.executeCommand("user-create", scannerWithInput(input), system);

        assertTrue(system.getUserManager().exists("testuser"));
    }

    @Test
    @DisplayName("Should show error for non-existent user")
    void shouldShowErrorForNonExistentUser() {
        String input = "nonexistent\n";

        parser.executeCommand("user-view", scannerWithInput(input), system);

        assertTrue(getOutput().contains("not found"));
    }

    @Test
    @DisplayName("Should execute stats command")
    void shouldExecuteStats() {
        parser.executeCommand("stats", scannerWithInput(""), system);

        String output = getOutput();
        assertAll(
                () -> assertTrue(output.contains("STATISTICS")),
                () -> assertTrue(output.contains("Users")),
                () -> assertTrue(output.contains("Roles"))
        );
    }

    @Test
    @DisplayName("Should execute help command")
    void shouldExecuteHelp() {
        parser.executeCommand("help", scannerWithInput(""), system);

        String output = getOutput();
        assertTrue(output.contains("AVAILABLE COMMANDS"));
    }

    @Test
    @DisplayName("Should view user with roles and permissions")
    void shouldViewUserDetails() {
        String input = "admin\n";

        parser.executeCommand("user-view", scannerWithInput(input), system);

        String output = getOutput();
        assertAll(
                () -> assertTrue(output.contains("admin")),
                () -> assertTrue(output.contains("Administrator")),
                () -> assertTrue(output.contains("Permissions"))
        );
    }

    @Test
    @DisplayName("Should search users")
    void shouldSearchUsers() {
        // Фильтр 1 by username, ввод "admin"
        String input = "1\nadmin\n";

        parser.executeCommand("user-search", scannerWithInput(input), system);

        String output = getOutput();
        assertTrue(output.contains("admin"));
    }

    @Test
    @DisplayName("Should list assignments")
    void shouldListAssignments() {
        parser.executeCommand("assignment-list", scannerWithInput(""), system);

        String output = getOutput();
        assertAll(
                () -> assertTrue(output.contains("admin")),
                () -> assertTrue(output.contains("Administrator")),
                () -> assertTrue(output.contains("PERMANENT"))
        );
    }
}