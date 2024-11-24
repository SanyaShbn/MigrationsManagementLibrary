import exception.LockException;
import executor.MigrationExecutor;
import executor.RollbackExecutor;
import reader.MigrationFileReader;
import utils.MigrationManager;

/** *
 * This class provides the application runner method
 * */
public class MigrationLibraryRunner {
    private static final String MIGRATIONS_DIRECTORY_PATH = "src/main/resources/db/migration";
    private static final String ROLLBACK_DIRECTORY_PATH = "src/main/resources/db/rollback";

    /** *
     * Starting the application
     * */
    public static void run() {
        MigrationFileReader migrationFileReader = new MigrationFileReader();
        MigrationManager migrationManager = new MigrationManager(migrationFileReader);
        MigrationExecutor migrationExecutor = new MigrationExecutor(migrationFileReader, migrationManager);
        RollbackExecutor rollbackExecutor = new RollbackExecutor(migrationFileReader, migrationManager);

        Runnable migrationTask = () -> {
            try {
                migrationExecutor.processMigrationFiles(MIGRATIONS_DIRECTORY_PATH);
//                rollbackExecutor.rollbackToVersion(ROLLBACK_DIRECTORY_PATH, 1);
            } catch (LockException e) {
                System.out.println("Caught exception: " + e.getMessage());
            }
        };

        Thread thread1 = new Thread(migrationTask);
        Thread thread2 = new Thread(migrationTask);
        Thread thread3 = new Thread(migrationTask);
        Thread thread4 = new Thread(migrationTask);
        Thread thread5 = new Thread(migrationTask);

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread5.start();
    }
}