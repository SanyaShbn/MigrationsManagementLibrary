import executor.MigrationExecutor;
import parser.RollbackSqlParser;
import reader.MigrationFileReader;
import utils.MigrationManager;

public class MigrationLibraryRunner {
    private static final String DIRECTORY_PATH = "src/main/resources/db/migration";
    public static void run(){
        MigrationFileReader migrationFileReader = new MigrationFileReader();
        MigrationManager migrationManager = new MigrationManager(migrationFileReader);
        RollbackSqlParser rollbackSqlParser = new RollbackSqlParser();
        MigrationExecutor migrationExecutor = new MigrationExecutor(migrationFileReader, migrationManager,
                rollbackSqlParser);
        migrationExecutor.processMigrationFiles(DIRECTORY_PATH);
    }
}
