package com.coniungo.app.dao;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PgDataService<T> implements  DatabaseService<T> {


    private static final String HOST = System.getenv("DB_HOST");
    private static final String PORT = System.getenv("DB_PORT");
    private static final String DB_NAME = System.getenv("DB_NAME");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    private static final HikariDataSource dataSource;

    static {
        // Build the JDBC URL dynamically from individual variables
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", HOST, PORT, DB_NAME);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(USER);
        config.setPassword(PASSWORD);


        // Lambda concurrency handles scaling; the pool doesn't need to.
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);

        // Optimization: Connection timeout
        config.setConnectionTimeout(5000);

        dataSource = new HikariDataSource(config);
    }


    @Override
    public void insert(String tableName, Map<String, Object> values, LambdaLogger logger) {
        logger.log("::::PgDataService.insert::::");
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Values cannot be null or empty.");
        }

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();

        values.forEach((key, value) -> {
            columns.append(key).append(",");
            placeholders.append("?,");
        });

        String sql = String.format(
                "INSERT INTO %s (%s) VALUES (%s);",
                tableName,
                columns.substring(0, columns.length() - 1),
                placeholders.substring(0, placeholders.length() - 1)
        );

        logger.log("INSERT STATEMENT: " + sql);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            int index = 1;
            for (Object value : values.values()) {
                if (value instanceof String) {
                    preparedStatement.setString(index++, String.valueOf(value));
                } else {
                    preparedStatement.setObject(index++, value);
                }
            }

            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println("Insert successful. Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            logger.log("INSERT ERROR: " + e.getMessage());
            throw new RuntimeException("Failed to insert record: " + e.getMessage(), e);
        }
    }


    @Override
    public T read(String tableName, String primaryKeyColumn, Object primaryKeyValue, RowMapper<T> rowMapper) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", tableName, primaryKeyColumn);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, primaryKeyValue);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return rowMapper.mapRow(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read record: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<T> readPaginated(
            String tableName,
            int pageSize,
            int pageNumber,
            RowMapper<T> rowMapper) {

        String sql = String.format("SELECT * FROM %s LIMIT ? OFFSET ?", tableName);

        List<T> results = new ArrayList<>();
        int offset = (pageNumber - 1) * pageSize;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, pageSize);
            preparedStatement.setInt(2, offset);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(rowMapper.mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read paginated records: " + e.getMessage(), e);
        }

        return results;
    }


    @Override
    public List<T> readMyRecordsPaginated(
            String tableNameOne,
            Integer userId,
            int pageSize,
            int pageNumber,
            RowMapper<T> rowMapper) {

        String sql = String.format("SELECT n.* " +
                "FROM %s n where n.userid = ? " +
                "LIMIT ? OFFSET ?", tableNameOne);

        List<T> results = new ArrayList<>();
        int offset = (pageNumber - 1) * pageSize;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, pageSize);
            preparedStatement.setInt(3, offset);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(rowMapper.mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read paginated records: " + e.getMessage(), e);
        }

        return results;
    }

    @Override
    public void update(String tableName, Map<String, Object> values, String whereClause, Object... whereParams) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Values cannot be null or empty.");
        }

        StringBuilder updateClause = new StringBuilder();
        values.forEach((key, value) -> updateClause.append(key).append(" = ?, "));

        String sql = String.format(
                "UPDATE %s SET %s WHERE %s",
                tableName,
                updateClause.substring(0, updateClause.length() - 2),
                whereClause
        );

        System.out.println("sql is " + sql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            int index = 1;
            for (Object value : values.values()) {
                preparedStatement.setObject(index++, value);
            }
            for (Object param : whereParams) {
                preparedStatement.setObject(index++, param);
            }

            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println("Update successful. Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update record: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String tableName, String whereClause, Object... whereParams) {
        String sql = String.format("DELETE FROM %s WHERE %s", tableName, whereClause);

        System.out.println("Delete sql " + sql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            int index = 1;
            for (Object param : whereParams) {
                preparedStatement.setObject(index++, param);
            }

            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println("Delete successful. Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete record: " + e.getMessage(), e);
        }
    }

    @Override
    public Object readColumnsByCondition(String tableName, String columnNames, String whereClause, String[] whereParams) {

        String sql = String.format("SELECT %s from %s where %s", columnNames, tableName, whereClause);
        System.out.println("Select sql " + sql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            int index = 1;
            for (Object param : whereParams) {
                preparedStatement.setObject(index++, param);
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read record: " + e.getMessage(), e);
        }
        return null;
    }
}
