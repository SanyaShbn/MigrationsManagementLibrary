package parser;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MigrationMetadata{
    private String version;
    private String description;
    private String installedBy;
}
