package executor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface Executor {
    //Метод для последовательного выполнения sql-кода из всех созданных файлов миграции
    boolean executeSql(Connection connection, List<String> sqlCommands, String script,
                       Integer version) throws SQLException;

}
