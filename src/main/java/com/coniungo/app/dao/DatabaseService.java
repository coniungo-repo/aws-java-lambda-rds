package com.coniungo.app.dao;


import com.amazonaws.services.lambda.runtime.LambdaLogger;

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
     * Reads a paginated list of records from the specified table.
     *
     * @param tableName the name of the table.
     * @param pageSize the number of records per page.
     * @param pageNumber the page number to retrieve.
     * @param rowMapper a functional interface to map the result set to entities.
     * @return a list of entities for the specified page.
     * @throws RuntimeException if an SQL error occurs during the read operation.
     */
    List<T> readPaginated(String tableName, int pageSize, int pageNumber, RowMapper<T> rowMapper);


    /**
     * Retrieves a paginated list of records from two specified tables in the database,
     * with an additional field indicating whether the record has been read by a specific user.
     *
     * @param tableNameOne the name of the first table (e.g., data table) containing the main records
     * @param tableNameTwo the name of the second table (e.g., datas_read table) containing user-specific read records
     * @param userId the ID of the user whose read records should be checked
     * @param pageSize the number of records to retrieve per page
     * @param pageNumber the page number to retrieve (1-based index)
     * @param rowMapper a mapper function to convert each {@link ResultSet} row into an instance of the desired type
     * @param <T> the type of objects to be returned in the resulting list
     * @return a list of records of type {@code T} with an additional field indicating whether the record has been read
     * @throws RuntimeException if a database access error occurs or if the SQL query execution fails
     *
     * <p><strong>Note:</strong> This method uses dynamic table names in the SQL query.
     * Ensure that the provided table names are sanitized or validated to prevent SQL injection vulnerabilities.</p>
     *
     * <h3>SQL Query Structure:</h3>
     * <pre>
     * SELECT n.data_id,
     *        n.message,
     *        n.created_at,
     *        CASE WHEN nr.id IS NOT NULL THEN true ELSE false END as datas_read
     * FROM [tableNameOne] n
     * LEFT JOIN [tableNameTwo] nr
     * ON n.data_id = nr.data_id AND nr.user_id = ?
     * LIMIT ? OFFSET ?
     * </pre>
     *
     * <h3>Pagination:</h3>
     * The method calculates the offset based on {@code pageSize} and {@code pageNumber}:
     * {@code offset = (pageNumber - 1) * pageSize}.
     *
     * <h3>Usage Example:</h3>
     * <pre>
     * List<Data> datas = readMyRecordsPaginated(
     *     "datas",
     *     "datas_read",
     *     "user123",
     *     10,
     *     1,
     *     resultSet -> new Data(
     *         resultSet.getInt("data_id"),
     *         resultSet.getString("message"),
     *         resultSet.getTimestamp("created_at").toLocalDateTime(),
     *         resultSet.getBoolean("datas_read")
     *     )
     * );
     * </pre>
     */
    List<T> readMyRecordsPaginated(
            String tableNameOne,
            Integer userId,
            int pageSize,
            int pageNumber,
            RowMapper<T> rowMapper);

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
    Object readColumnsByCondition(String tableName, String columns, String whereClause, String[] whereParams);
}