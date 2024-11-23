import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import utils.MigrationReportGenerator;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class MigrationReportGeneratorTest {

    @InjectMocks
    private MigrationReportGenerator reportGenerator;

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateCsvReport() throws SQLException {

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt("version")).thenReturn(1);
        when(mockResultSet.getString("description")).thenReturn("Initial migration");
        when(mockResultSet.getString("script")).thenReturn("V1__init.sql");
        when(mockResultSet.getInt("checksum")).thenReturn(12345);
        when(mockResultSet.getTimestamp("installed_on")).thenReturn(new java.sql.Timestamp(System.currentTimeMillis()));
        when(mockResultSet.getString("installed_by")).thenReturn("user");
        when(mockResultSet.getInt("execution_time")).thenReturn(100);
        when(mockResultSet.getBoolean("success")).thenReturn(true);
        when(mockResultSet.getString("status")).thenReturn("applied");

        reportGenerator.generateCsvReport("test_report.csv");

        File file = new File("test_report.csv");
        assertTrue(file.exists());

        file.delete();
    }

    @Test
    void testGenerateJsonReport() throws SQLException {

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt("version")).thenReturn(1);
        when(mockResultSet.getString("description")).thenReturn("Initial migration");
        when(mockResultSet.getString("script")).thenReturn("V1__init.sql");
        when(mockResultSet.getInt("checksum")).thenReturn(12345);
        when(mockResultSet.getTimestamp("installed_on")).thenReturn(new java.sql.Timestamp(System.currentTimeMillis()));
        when(mockResultSet.getString("installed_by")).thenReturn("user");
        when(mockResultSet.getInt("execution_time")).thenReturn(100);
        when(mockResultSet.getBoolean("success")).thenReturn(true);
        when(mockResultSet.getString("status")).thenReturn("applied");

        reportGenerator.generateJsonReport("test_report.json");

        File file = new File("test_report.json");
        assertTrue(file.exists());

        file.delete();
    }
}
