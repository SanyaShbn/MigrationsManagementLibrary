import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reader.MigrationFileReader;
import utils.MigrationManager;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class MigrationManagerTest {
    @Mock
    private MigrationFileReader migrationFileReader;
    @InjectMocks
    private MigrationManager migrationManager;
    @Mock
    private Connection connection;
    @Mock
    private Statement statement;
    @Mock
    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAndSortMigrationFiles() {
        when(migrationFileReader.findDbMigrationFiles(anyString())).thenReturn(List.of(new File("V1__init.sql"), new File("V2__update.sql")));

        List<File> files = migrationManager.findAndSortMigrationFiles("src/test/resources/db");

        assertEquals(2, files.size());
        assertEquals("V1__init.sql", files.get(0).getName());
    }

    @Test
    void testGetCurrentVersion() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("version")).thenReturn("V1");

        String currentVersion = migrationManager.getCurrentVersion(connection);

        assertEquals("V1", currentVersion);
    }

    @Test
    void testShouldApplyMigration() {
        assertTrue(migrationManager.shouldApplyMigration(null, "V1"));
        assertTrue(migrationManager.shouldApplyMigration("V1", "V2"));
        assertFalse(migrationManager.shouldApplyMigration("V2", "V1"));
    }

    @Test
    void testExtractVersionFromFilename() {
        String version = migrationManager.extractVersionFromFilename(new File("V1__init.sql"));
        assertEquals("1", version);
    }
}