package parser;

import lombok.Getter;
import lombok.Setter;

/** *
 * This class is used for mapping metadata (author, description) about the migration from sql file
 * */
@Setter
@Getter
public class MigrationMetadata{
    private String description;
    private String installedBy;
}
