import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import processor.MigrationFileProcessor;
import reader.MigrationFileReader;
import utils.ConnectionManager;

public class MigrationFileProcessorIntegrationTest {

    private MigrationFileProcessor migrationFileProcessor;
    private MigrationFileReader migrationFileReader;

    @BeforeEach
    public void setUp() throws Exception {

        migrationFileReader = new MigrationFileReader();
        migrationFileProcessor = new MigrationFileProcessor(migrationFileReader);

        File migrationFile = new File("src/test/resources/db/test_migration.sql");
        migrationFile.getParentFile().mkdirs();
        migrationFile.createNewFile();
        Files.write(migrationFile.toPath(), List.of("-- ashubin -- v1 -- Initial setup\nCREATE TABLE test_table" +
                " (id SERIAL PRIMARY KEY);"));
    }

    @AfterEach
    public void tearDown() throws IOException{
        Files.deleteIfExists(new File("src/test/resources/db/test_migration.sql").toPath());
    }

    @Test
    public void testProcessMigrationFiles() throws IOException, SQLException {
        String directoryPath = "src/test/resources/db";

        migrationFileProcessor.processMigrationFiles("src/test/resources/db");

        try (var connection = ConnectionManager.get();
             var statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM information_schema.tables" +
                    " WHERE table_name = 'test_table'");
            assertTrue(resultSet.next());
        }
    }
}