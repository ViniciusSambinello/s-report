package s.reports.common.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import javax.sql.DataSource;

abstract class AbstractRepository {

    private final DataSource dataSource;
    private final String tablePrefix;
    private final RepositoryExecutor executor;
    private final DatabaseAvailability availability;

    AbstractRepository(DataSource dataSource, String tablePrefix, RepositoryExecutor executor, DatabaseAvailability availability) {
        this.dataSource = dataSource;
        this.tablePrefix = tablePrefix;
        this.executor = executor;
        this.availability = availability;
    }

    interface SqlOperation<T> {
        T run(Connection connection) throws SQLException;
    }

    final <T> CompletableFuture<T> execute(SqlOperation<T> operation) {
        return executor.submit(() -> {
            try (Connection connection = dataSource.getConnection()) {
                final T result = operation.run(connection);
                availability.reportSuccess();
                return result;
            } catch (SQLException exception) {
                availability.reportFailure(exception);
                throw new StorageUnavailableException(exception);
            }
        });
    }

    final String tableName(String suffix) {
        return "`" + tablePrefix + suffix + "`";
    }
}
