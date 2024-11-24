package utils;

import java.io.File;
import java.sql.Connection;
import java.util.List;

/** *
 * Utility class for handling null values and empty values
 * */
public class Validator {
    public static void checkNotNull(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Error: Connection is closed");
        }
    }
    public static void checkNotNull(String str, String name) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    public static void checkNotNull(Integer str, String name) {
        if (str == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    public static void checkNotNull(List<String> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("Found no sql commands for executing migrations");
        }
    }

    /** *
     * Checking the presence of the necessary metadata in comments for the migration's 'author'
     * and 'descriptions' fields
     *
     * @param installedBy 'installed_by' field value of potential new record in the database
     * @param description 'description' field value of potential new record in the database
     * */
    public static void checkNotNullMigrationAuthorAndDescription(String installedBy, String description) {
        if (installedBy == null || installedBy.isEmpty()) {
            throw new IllegalArgumentException("Error processing migration files. You must identify the author" +
                    " of the migration in the comments");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Error processing migration files. You must identify the description" +
                    " of the migration in the comments");
        }
    }

    public static void checkFileExists(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Provided path is not a valid file: " +
                    (file != null ? file.getAbsolutePath() : "null"));
        }
    }

    /** *
     * Checking the correctness of migration files and rollbacks names
     *
     * @param file analyzed file
     * */
    public static void checkMigrationFileFormat(File file) {
        checkFileExists(file);
        if (!file.getName().matches("V[0-9]+__.*\\.sql") &&
                !file.getName().matches("U[0-9]+__rollback_V[0-9]+__.*\\.sql")) {
            throw new IllegalArgumentException("Invalid migration file format: " + file.getName());
        }
    }

}


