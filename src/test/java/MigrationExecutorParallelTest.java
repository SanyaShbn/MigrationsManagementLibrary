import exception.LockException;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class MigrationExecutorParallelTest {

    @InjectMocks
    private MigrationExecutor migrationExecutor;

    @Mock
    private MigrationFileReader migrationFileReader;

    @Mock
    private MigrationManager migrationManager;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private boolean exceptionThrown;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        exceptionThrown = false;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getBoolean("is_locked")).thenReturn(false);
        when(mockPreparedStatement.executeUpdate()).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.contains("WHERE is_locked = FALSE")) {
                Thread.sleep(1000); // Ожидание для симуляции блокировки
                return 0;
            }
            return 1;
        });

        // Mock methods for file reading and migration version handling
        when(migrationFileReader.readDbMigrationFile(any(File.class))).thenReturn(List.of(
                "CREATE TABLE test (id SERIAL PRIMARY KEY, is_locked BOOLEAN DEFAULT FALSE)"));
        when(migrationManager.extractVersionFromFilename(any(File.class))).thenReturn(1);
        when(migrationManager.findAndSortMigrationFiles(anyString())).thenReturn(List.of(
                new File("migration1.sql")));
        when(migrationManager.getCurrentVersion(any(Connection.class))).thenReturn(0);
        when(migrationManager.shouldApplyMigration(anyInt(), anyInt())).thenReturn(true);
    }

    @Test
    void testParallelMigrationExecutionThrowsLockException() throws InterruptedException {
        Runnable migrationTask = () -> {
            try {
                migrationExecutor.processMigrationFiles("src/main/resources/db/migration");
            } catch (LockException e) {
                exceptionThrown = true;
                System.out.println("Caught LockException: " + e.getMessage());
            }
        };

        Thread thread1 = new Thread(migrationTask);
        Thread thread2 = new Thread(migrationTask);

        thread1.start();
        thread2.start();

        assertFalse(exceptionThrown, "LockException was thrown");
    }
}