package utils;

import lombok.extern.slf4j.Slf4j;
import reader.MigrationFileReader;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class MigrationManager {
    private final MigrationFileReader migrationFileReader;

    public MigrationManager(MigrationFileReader migrationFileReader) {
        this.migrationFileReader = migrationFileReader;
    }

    public List<File> findAndSortMigrationFiles(String directoryPath) {
        List<File> migrationFiles = migrationFileReader.findDbMigrationFiles(directoryPath);
        validateFileFormat(migrationFiles);
        List<File> mutableMigrationFiles = new ArrayList<>(migrationFiles);

        mutableMigrationFiles.sort(Comparator.comparing(this::extractVersionFromFilename));
        return mutableMigrationFiles;
    }

    private void validateFileFormat(List<File> files) {
        for (File file : files) {
            if (!file.getName().matches("V[0-9]+__.*\\.sql")) {
                throw new IllegalArgumentException("Invalid migration file format: " + file.getName());
            }
        }
    }

    public String getCurrentVersion(Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     """
                            SELECT version FROM schema_history_table
                            ORDER BY installed_on DESC LIMIT 1
                         """)){
            if (resultSet.next()) {
                return resultSet.getString("version");
            }
        } catch (SQLException e) {
            log.error("Error retrieving current database version: ", e);
        }
        return null;
    }

    public boolean shouldApplyMigration(String currentVersion, String scriptVersion) {
        return currentVersion == null || scriptVersion.compareTo(currentVersion) > 0;
    }

    public String extractVersionFromFilename(File file) {
        return file.getName().split("__")[0].substring(1);
    }
}
