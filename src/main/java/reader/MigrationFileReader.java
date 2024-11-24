package reader;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static utils.Validator.checkFileExists;

/** *
 * Provides the realization of FileReader interface methods for analyzing proper directories
 * and reading migration/rollback files from this directories
 * */
@Slf4j
public class MigrationFileReader implements FileReader{

    /** *
     * Method for searching .sql files in project's resources
     *
     * @param path the path to the proper directory
     * @return list of files from analyzed directory
     * */
    @Override
    public List<File> findDbMigrationFiles(String path) {
        List<File> migrationFiles = new ArrayList<>();
        try {
            File directory = new File(path);
            if (!directory.exists() || !directory.isDirectory()) {
                throw new IOException("Directory not found: " + path);
            }
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".sql"));
            if (files != null) {
                migrationFiles.addAll(Arrays.asList(files));
            }
        }catch (IOException e) {
            log.error("Error analyzing directory for migrations: " + e.getMessage());
        }
        return migrationFiles;
    }

    /** *
     * Method for reading the content of each of the files
     *
     * @param file the read file
     * @return list of strings with sql-commands from the file
     * */
    @Override
    public List<String> readDbMigrationFile(File file) {
        checkFileExists(file);
        try {
            String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
            return Arrays.asList(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}