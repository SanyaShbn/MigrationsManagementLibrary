package parser;

/** *
 * This class is used for parsing metadata (author, description) about the migration
 * into a proper mapper class
 * */
public class MigrationMetadataParser {

    /** *
     * Parsing the migration metadata from sql-comments in .sql files
     *
     * @param sql sql code to parse into a proper mapper class
     * @return MigrationMetadata object for further migrations executing
     * */
    public static MigrationMetadata parseMigrationMetadata(String sql) {
        MigrationMetadata metadata = new MigrationMetadata();
        String[] lines = sql.split("\n");

        for (String line : lines) {
            if (line.trim().startsWith("--")) {
                String[] tokens = line.split("--");
                for (String token : tokens) {
                    token = token.trim();
                    if (!token.isEmpty()) {
                        if (metadata.getInstalledBy() == null) {
                            metadata.setInstalledBy(token);
                        } else {
                            metadata.setDescription(token);
                        }
                    }
                }
                break;
            }
        }
        return metadata;
    }
}
