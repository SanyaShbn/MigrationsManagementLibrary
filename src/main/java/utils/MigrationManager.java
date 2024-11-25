package utils;

import lombok.extern.slf4j.Slf4j;
import reader.MigrationFileReader;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** *
 * Utility class for managing migrations and rollbacks
 * */
@Slf4j
public class MigrationManager {

    private static final String ROLLBACK_FILE_PATTERN = "U[0-9]+__rollback_V" + "%d" + "__.*\\.sql";
    private final MigrationFileReader migrationFileReader;

    public MigrationManager(MigrationFileReader migrationFileReader) {
        this.migrationFileReader = migrationFileReader;
    }

    /** *
     * Searching for the migration files in given directory
     *
     * @param directoryPath the directory path for .sql files
     * @return list of found files
     * */
    public List<File> findAndSortMigrationFiles(String directoryPath) {
        List<File> migrationFiles = migrationFileReader.findDbMigrationFiles(directoryPath);
        validateFileFormat(migrationFiles);
        List<File> mutableMigrationFiles = new ArrayList<>(migrationFiles);

        mutableMigrationFiles.sort(Comparator.comparing(this::extractVersionFromFilename));
        return mutableMigrationFiles;
    }

    /** *
     * Getting the current database version
     *
     * @param connection the connection to your database
     * @return version's number
     * */
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

    /** *
     * Comparing the migration version with the current database version
     *
     * @param currentVersion current database version's number
     * @param scriptVersion scripts version's number (getting form file name)
     * @return true if current version is less then script version, and false if higher
     * */
    public boolean shouldApplyMigration(Integer currentVersion, Integer scriptVersion) {
        return currentVersion == null || scriptVersion > currentVersion;
    }

    /** *
     * Getting the script's version from .sql files names
     *
     * @param file given file to find out it's version
     * @return version's number
     * */
    public Integer extractVersionFromFilename(File file) {
        return Integer.valueOf(file.getName().split("__")[0].substring(1));
    }

    /** *
     * Finding a specific rollback file to use cherryPick rollback
     *
     * @param directoryPath the given directory path for searching the file
     * @param scriptVersion scripts version's number (getting form file name)
     * @return found file
     * */
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

    /**
     * Возвращает список применённых миграций.
     *
     * @param connection the connection to your database.
     * @return list of applied migrations.
     * @throws SQLException when database modification error occurs.
     */
    public List<String> getAppliedMigrations(Connection connection) throws SQLException {
        List<String> appliedMigrations = new ArrayList<>();

        String query = "SELECT script FROM schema_history_table WHERE status = 'applied' ORDER BY version ASC";
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String migrationScript = resultSet.getString("script");
                appliedMigrations.add(migrationScript);
            }
        }

        return appliedMigrations;
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
}
