package parser;

public class MigrationMetadataParser {
    public static MigrationMetadata parseMigrationMetadata(String sql) {
        MigrationMetadata metadata = new MigrationMetadata();
        String[] lines = sql.split("\n");

        for (String line : lines) {
            if (line.trim().startsWith("--")) {
                String[] tokens = line.split("--");
                for (String token : tokens) {
                    token = token.trim();
                    if (token.startsWith("v")) {
                        metadata.setVersion(token);
                    } else if (!token.isEmpty()) {
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
