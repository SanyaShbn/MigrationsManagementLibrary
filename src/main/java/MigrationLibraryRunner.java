import executor.MigrationExecutor;
import executor.RollbackExecutor;
import reader.MigrationFileReader;
import utils.MigrationManager;

public class MigrationLibraryRunner {
    private static final String MIGRATIONS_DIRECTORY_PATH = "src/main/resources/db/migration";
    private static final String ROLLBACKS_DIRECTORY_PATH = "src/main/resources/db/rollback";
    public static void run(){

        MigrationFileReader migrationFileReader = new MigrationFileReader();
        MigrationManager migrationManager = new MigrationManager(migrationFileReader);
        MigrationExecutor migrationExecutor = new MigrationExecutor(migrationFileReader, migrationManager);

        migrationExecutor.processMigrationFiles(MIGRATIONS_DIRECTORY_PATH);

        RollbackExecutor rollbackExecutor = new RollbackExecutor(migrationFileReader, migrationManager);
//        rollbackExecutor.rollbackToVersion(ROLLBACKS_DIRECTORY_PATH, 1);
//        rollbackExecutor.cherryPickRollback(ROLLBACKS_DIRECTORY_PATH, 2);

    }
}