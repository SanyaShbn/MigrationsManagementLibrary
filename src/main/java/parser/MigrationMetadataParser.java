package parser;

//В качестве метаданных посчитал данные, указываемые в комментариях в .sql файлах
//В комментариях нужно указывать автора миграции (поле installed_by в schema_history_table)
//и описание (description)
public class MigrationMetadataParser {
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
