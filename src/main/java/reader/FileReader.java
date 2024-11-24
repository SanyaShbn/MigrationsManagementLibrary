package reader;

import java.io.File;
import java.util.List;

/** *
 * Provides classes with the methods for analyzing proper directories and reading migration/rollback
 * files from this directories
 * */
public interface FileReader {
    List<File> findDbMigrationFiles(String path);
    List<String> readDbMigrationFile(File file);
}
