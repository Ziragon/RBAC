package command;

import com.example.command.CommandParser;
import com.example.entity.Role;
import com.example.system.RBACSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandParser Tests")
class CommandParserTest {

    private CommandParser parser;
    private RBACSystem system;
    private StringBuilder output;

    @BeforeEach
    void setUp() {
        Role.clearNameRegistry();
        parser = new CommandParser();
        system = new RBACSystem();
        output = new StringBuilder();
    }

    @Test
    @DisplayName("Should register and execute command")
    void shouldRegisterAndExecuteCommand() {
        parser.registerCommand("test", "Test command",
                (scanner, sys) -> output.append("executed"));

        parser.executeCommand("test", new Scanner(""), system);

        assertEquals("executed", output.toString());
    }

    @Test
    @DisplayName("Should handle unknown command")
    void shouldHandleUnknownCommand() {
        parser.registerCommand("valid", "Valid command", (s, sys) -> {});

        assertAll(
                () -> assertFalse(parser.hasCommand("unknown")),
                () -> assertDoesNotThrow(() ->
                        parser.executeCommand("unknown", new Scanner(""), system))
        );
    }

    @Test
    @DisplayName("Should parse and execute input")
    void shouldParseAndExecuteInput() {
        parser.registerCommand("greet", "Greeting",
                (scanner, sys) -> output.append("hello"));

        parser.parseAndExecute("  GREET  extra args  ", new Scanner(""), system);

        assertEquals("hello", output.toString());
    }

    @Test
    @DisplayName("Should throw for invalid registration")
    void shouldThrowForInvalidRegistration() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> parser.registerCommand(null, "desc", (s, sys) -> {})),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> parser.registerCommand("cmd", "desc", null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> parser.registerCommand("  ", "desc", (s, sys) -> {}))
        );
    }

    @Test
    @DisplayName("Should count registered commands")
    void shouldCountRegisteredCommands() {
        assertEquals(0, parser.getCommandCount());

        parser.registerCommand("cmd1", "First", (s, sys) -> {});
        parser.registerCommand("cmd2", "Second", (s, sys) -> {});

        assertEquals(2, parser.getCommandCount());
        assertTrue(parser.hasCommand("cmd1"));
        assertTrue(parser.hasCommand("CMD2"));
    }
}