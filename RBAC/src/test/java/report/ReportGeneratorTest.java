package report;

import com.example.assignment.PermanentAssignment;
import com.example.entity.*;
import com.example.report.ReportGenerator;
import com.example.repository.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReportGenerator Tests")
class ReportGeneratorTest {

    private ReportGenerator generator;
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    @BeforeEach
    void setUp() {
        Role.clearNameRegistry();
        generator = new ReportGenerator();
        assignmentManager = new AssignmentManager();
        roleManager = new RoleManager(assignmentManager);
        userManager = new UserManager();

        Permission read = new Permission("READ", "users", "View");
        Role role = Role.create("TestRole_" + System.nanoTime(), "Test", Set.of(read));
        roleManager.add(role);

        User user = new User("testuser", "Test User", "test@test.com");
        userManager.add(user);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "Test");
        assignmentManager.add(new PermanentAssignment(user, role, meta));
    }

    @Test
    @DisplayName("Should generate user report")
    void shouldGenerateUserReport() {
        String report = generator.generateUserReport(userManager, assignmentManager);

        assertAll(
                () -> assertTrue(report.contains("USER REPORT")),
                () -> assertTrue(report.contains("testuser")),
                () -> assertTrue(report.contains("READ"))
        );
    }

    @Test
    @DisplayName("Should generate role report")
    void shouldGenerateRoleReport() {
        String report = generator.generateRoleReport(roleManager, assignmentManager);

        assertAll(
                () -> assertTrue(report.contains("ROLE REPORT")),
                () -> assertTrue(report.contains("TestRole"))
        );
    }

    @Test
    @DisplayName("Should generate permission matrix")
    void shouldGeneratePermissionMatrix() {
        String report = generator.generatePermissionMatrix(userManager, assignmentManager);

        assertAll(
                () -> assertTrue(report.contains("PERMISSION MATRIX")),
                () -> assertTrue(report.contains("testuser"))
        );
    }

    @Test
    @DisplayName("Should export report to file")
    void shouldExportToFile() throws Exception {
        String report = generator.generateUserReport(userManager, assignmentManager);
        String filename = "test_report.txt";

        generator.exportToFile(report, filename);

        File file = new File(filename);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        file.delete();
    }
}