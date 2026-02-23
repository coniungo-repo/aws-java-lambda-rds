package com.coniungo.app.dao;


import com.amazonaws.services.lambda.runtime.LambdaLogger;


import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Interface defining generic data access operations.
 *
 * @param <T> the type of the entity this service manages.
 */
public interface DatabaseService<T> {


    /**
     * Inserts a new record into the specified table.
     *
     * @param tableName the name of the table.
     * @param values a map of column names to values to insert.
     * @throws IllegalArgumentException if {@code values} is null or empty.
     * @throws RuntimeException if an SQL error occurs during the insert operation.
     */
    void insert(String tableName, Map<String, Object> values, LambdaLogger logger);

    /**
     * Reads a single record from the specified table using the primary key.
     *
     * @param tableName the name of the table.
     * @param primaryKeyColumn the name of the primary key column.
     * @param primaryKeyValue the value of the primary key.
     * @param rowMapper a functional interface to map the result set to an entity.
     * @return the mapped entity, or {@code null} if no record is found.
     * @throws RuntimeException if an SQL error occurs during the read operation.
     */
    T read(String tableName, String primaryKeyColumn, Object primaryKeyValue, RowMapper<T> rowMapper);



    /**
     * Updates records in the specified table.
     *
     * @param tableName the name of the table.
     * @param values a map of column names to updated values.
     * @param whereClause the SQL WHERE clause to specify which rows to update.
     * @param whereParams parameters for the WHERE clause.
     * @throws IllegalArgumentException if {@code values} is null or empty.
     * @throws RuntimeException if an SQL error occurs during the update operation.
     */
    void update(String tableName, Map<String, Object> values, String whereClause, Object... whereParams);

    /**
     * Deletes records from the specified table.
     *
     * @param tableName the name of the table.
     * @param whereClause the SQL WHERE clause to specify which rows to delete.
     * @param whereParams parameters for the WHERE clause.
     * @throws RuntimeException if an SQL error occurs during the delete operation.
     */
    void delete(String tableName, String whereClause, Object... whereParams);




    /**
     * Functional interface to map a {@link ResultSet} row to an entity.
     *
     * @param <T> the type of the entity.
     */
    @FunctionalInterface
    interface RowMapper<T> {
        /**
         * Maps a single row of the given {@link ResultSet} to an entity.
         *
         * @param resultSet the result set to map.
         * @return the mapped entity.
         * @throws SQLException if an error occurs accessing the result set.
         */
        T mapRow(ResultSet resultSet) throws SQLException;
    }
    Object readColumnsByCondition(String tableName, String columns, String whereClause, Object[] whereParams);




    void withTransaction(TransactionCallback callback);


    void insert(Connection conn,
                String tableName,
                Map<String, Object> values,
                LambdaLogger logger) throws SQLException;

    void update(Connection conn,
                String tableName,
                Map<String, Object> values,
                String whereClause,
                Object... whereParams) throws SQLException;

    Object readColumnsByCondition(Connection conn,
                                  String tableName,
                                  String columns,
                                  String whereClause,
                                  Object[] whereParams) throws SQLException;

    List<T> readPaginated(
            String tableName,
            int pageSize,
            int pageNumber,
            RowMapper<T> mapper
    );

    @FunctionalInterface
    interface TransactionCallback {
        void execute(Connection connection) throws Exception;
    }
}