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

    private static final String ROLLBACK_FILE_PATTERN = "U[0-9]+__rollback_V" + "%d" + "__.*\\.sql";
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
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No migration files found in the path");
        }

        for (File file : files) {
            Validator.checkFileExists(file);
            Validator.checkMigrationFileFormat(file);
        }
    }

    // Получение текущей версии БД
    public Integer getCurrentVersion(Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     """
                            SELECT MAX(version) FROM schema_history_table WHERE script LIKE 'V%'
                            AND status = 'applied'
                         """)){
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error retrieving current database version: ", e);
        }
        return null;
    }

    // Сравнение версии миграции и БД
    public boolean shouldApplyMigration(Integer currentVersion, Integer scriptVersion) {
        return currentVersion == null || scriptVersion > currentVersion;
    }

    // Версия указывается в названии файла, при помощи этого метода считываю ее, чтобы понять,
    // нужно ли применять данную миграцию еще раз
    public Integer extractVersionFromFilename(File file) {
        return Integer.valueOf(file.getName().split("__")[0].substring(1));
    }

    // Поиск конкретного rollback-файла для применения cherryPick rollback-а
    public File findRollbackFileByVersion(String directoryPath, int scriptVersion) {
        File dir = new File(directoryPath);
        String pattern = String.format(ROLLBACK_FILE_PATTERN, scriptVersion);
        // Поиск файла
        File[] files = dir.listFiles((dir1, name) -> name.matches(pattern));
        if (files != null && files.length > 0) {
            return files[0];
        } else {
            return null;
        }
    }
}
