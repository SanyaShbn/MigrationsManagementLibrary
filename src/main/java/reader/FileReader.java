package reader;

import java.io.File;
import java.util.List;

public interface FileReader {
    List<File> findDbMigrationFiles(String path);
    List<String> readDbMigrationFile(File file);
}
