import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import parser.MigrationMetadata;
import parser.MigrationMetadataParser;
import processor.MigrationFileProcessor;
import reader.MigrationFileReader;

public class MigrationFileProcessorTest {

    @Mock
    private MigrationFileReader migrationFileReader;

    @InjectMocks
    private MigrationFileProcessor migrationFileProcessor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testParseMigrationMetadata() {
        String sql = "-- ashubin -- v1 -- Initial setup\nCREATE TABLE test_table (id SERIAL PRIMARY KEY);";
        MigrationMetadata metadata = MigrationMetadataParser.parseMigrationMetadata(sql);
        assertEquals("ashubin", metadata.getInstalledBy());
        assertEquals("v1", metadata.getVersion());
        assertEquals("Initial setup", metadata.getDescription());
    }

    @Test
    public void testProcessMigrationFiles() throws IOException {
        File mockFile = mock(File.class);
        when(mockFile.getName()).thenReturn("test_migration.sql");
        when(migrationFileReader.findDbMigrationFiles(anyString())).thenReturn(Collections.singletonList(mockFile));
        when(migrationFileReader.readDbMigrationFile(mockFile)).thenReturn(List.of(
                "-- ashubin -- v1 -- Initial setup\nCREATE TABLE test_table (id SERIAL PRIMARY KEY);"));

        migrationFileProcessor.processMigrationFiles("src/test/resources/db");

        verify(migrationFileReader, times(1)).findDbMigrationFiles(anyString());
        verify(migrationFileReader, times(1)).readDbMigrationFile(mockFile);
    }
}


