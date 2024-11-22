import executor.MigrationExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reader.MigrationFileReader;
import utils.MigrationManager;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MigrationExecutorTest {

    @Mock
    private MigrationFileReader migrationFileReader;

    @Mock
    private MigrationManager migrationManager;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @InjectMocks
    private MigrationExecutor migrationExecutor;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(mock(java.sql.ResultSet.class));
    }
    @Test
    void testProcessMigrationFiles_successful() throws SQLException {
        List<File> migrationFiles = List.of(new File("V1__init.sql"), new File("V2__update.sql"));
        when(migrationManager.findAndSortMigrationFiles(anyString())).thenReturn(migrationFiles);
        when(migrationFileReader.readDbMigrationFile(any(File.class))).
                thenReturn(List.of("""
                                           CREATE TABLE test (
                                           id SERIAL PRIMARY KEY
                                           );
                                       """));
        when(migrationManager.getCurrentVersion(any(Connection.class))).thenReturn(1);
        when(migrationManager.extractVersionFromFilename(any(File.class))).thenReturn(2);
        when(migrationManager.shouldApplyMigration(anyInt(), anyInt())).thenReturn(true);

        migrationExecutor.processMigrationFiles("src/test/resources/db");

        verify(connection).commit();
    }
    @Test
    void testProcessMigrationFiles_rollbackOnFailure() throws SQLException {
        List<File> migrationFiles = List.of(new File("V1__init.sql"), new File("V2__update.sql"));
        when(migrationManager.findAndSortMigrationFiles(anyString())).thenReturn(migrationFiles);
        when(migrationFileReader.readDbMigrationFile(any(File.class))).thenReturn(List.of("SQL COMMAND"));
        when(migrationManager.getCurrentVersion(any(Connection.class))).thenReturn(1);
        when(migrationManager.extractVersionFromFilename(any(File.class))).thenReturn(2);
        when(migrationManager.shouldApplyMigration(anyInt(), anyInt())).thenReturn(true);
        doThrow(new SQLException("Test exception")).when(statement).execute(anyString());

        migrationExecutor.processMigrationFiles("src/test/resources/db");

        verify(connection).rollback();
    }

}
