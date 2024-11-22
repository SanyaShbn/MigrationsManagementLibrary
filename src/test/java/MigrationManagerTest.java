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
        when(migrationFileReader.findDbMigrationFiles(anyString())).thenReturn(List.of(
                new File("src/test/resources/db/V1__init.sql"),
                new File("src/test/resources/db/V2__update.sql")));

        List<File> files = migrationManager.findAndSortMigrationFiles("src/test/resources/db");

        assertEquals(2, files.size());
        assertEquals("V1__init.sql", files.get(0).getName());
    }

    @Test
    void testGetCurrentVersion() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        Integer currentVersion = migrationManager.getCurrentVersion(connection);

        assertEquals(1, currentVersion);
    }

    @Test
    void testShouldApplyMigration() {
        assertTrue(migrationManager.shouldApplyMigration(null, 1));
        assertTrue(migrationManager.shouldApplyMigration(1, 2));
        assertFalse(migrationManager.shouldApplyMigration(2, 1));
    }

    @Test
    void testExtractVersionFromFilename() {
        Integer version = migrationManager.extractVersionFromFilename(new File("V1__init.sql"));
        assertEquals(1, version);
    }
}