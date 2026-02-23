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


    private static volatile HikariDataSource dataSource;

    private static HikariDataSource getDataSource() {

        if (dataSource != null) {
            return dataSource;
        }

        synchronized (PgDataService.class) {

            if (dataSource != null) {
                return dataSource;
            }

            String host = System.getenv("DB_HOST");
            String port = System.getenv("DB_PORT");
            String db   = System.getenv("DB_NAME");
            String user = System.getenv("DB_USER");
            String pass = System.getenv("DB_PASSWORD");

            if (host == null || db == null) {
                throw new IllegalStateException("DB env vars missing");
            }

            String jdbcUrl =
                    "jdbc:postgresql://" + host + ":" + port + "/" + db;

            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(user);
            config.setPassword(pass);

            config.setMaximumPoolSize(2);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(5000);
            config.setInitializationFailTimeout(-1);

            dataSource = new HikariDataSource(config);

            return dataSource;
        }
    }


    @Override
    public void withTransaction(TransactionCallback callback) {

        try (Connection conn = getDataSource().getConnection()) {

            conn.setAutoCommit(false);

            try {
                callback.execute(conn);
                conn.commit();
            } catch (Exception e) {

                conn.rollback();
                throw new RuntimeException("Transaction failed", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Transaction error", e);
        }
    }



    @Override
    public void insert(String table,
                       Map<String, Object> values,
                       LambdaLogger logger) {

        try (Connection conn = getDataSource().getConnection()) {

            insert(conn, table, values, logger);

        } catch (SQLException e) {

            throw new RuntimeException("Insert failed", e);
        }
    }


    @Override
    public void insert(Connection conn,
                       String table,
                       Map<String, Object> values,
                       LambdaLogger logger) throws SQLException {

        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Empty values");
        }

        StringBuilder cols = new StringBuilder();
        StringBuilder qms  = new StringBuilder();

        values.forEach((k,v)->{
            cols.append(k).append(",");
            qms.append("?,");
        });

        String sql = String.format(
                "INSERT INTO %s (%s) VALUES (%s)",
                table,
                cols.substring(0, cols.length()-1),
                qms.substring(0, qms.length()-1)
        );

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            int i = 1;

            for (Object v : values.values()) {
                ps.setObject(i++, v);
            }

            ps.executeUpdate();
        }
    }



    @Override
    public void update(String table,
                       Map<String,Object> values,
                       String where,
                       Object... params) {

        try (Connection conn = getDataSource().getConnection()) {

            update(conn, table, values, where, params);

        } catch (SQLException e) {

            throw new RuntimeException("Update failed", e);
        }
    }


    @Override
    public void update(Connection conn,
                       String table,
                       Map<String,Object> values,
                       String where,
                       Object... params) throws SQLException {

        StringBuilder set = new StringBuilder();

        values.forEach((k,v)->
                set.append(k).append("=?,")
        );

        String sql = String.format(
                "UPDATE %s SET %s WHERE %s",
                table,
                set.substring(0, set.length()-1),
                where
        );

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            int i = 1;

            for (Object v : values.values()) {
                ps.setObject(i++, v);
            }

            for (Object p : params) {
                ps.setObject(i++, p);
            }

            ps.executeUpdate();
        }
    }


    @Override
    public T read(String table,
                  String pkCol,
                  Object pkVal,
                  RowMapper<T> mapper) {

        String sql =
                "SELECT * FROM " + table +
                        " WHERE " + pkCol + "=?";

        try (Connection conn = getDataSource().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setObject(1, pkVal);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return mapper.mapRow(rs);
                }
            }

        } catch (Exception e) {

            throw new RuntimeException("Read failed", e);
        }

        return null;
    }


    @Override
    public List<T> readPaginated(
            String table,
            int pageSize,
            int pageNumber,
            RowMapper<T> mapper
    ) {

        int offset = (pageNumber - 1) * pageSize;

        String sql =
                "SELECT * FROM " + table +
                        " LIMIT ? OFFSET ?";

        List<T> results = new ArrayList<>();

        try (Connection conn = getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pageSize);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    results.add(mapper.mapRow(rs));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Paginated read failed", e);
        }

        return results;
    }


    @Override
    public Object readColumnsByCondition(String table,
                                         String cols,
                                         String where,
                                         Object[] params) {

        try (Connection conn = getDataSource().getConnection()) {

            return readColumnsByCondition(
                    conn, table, cols, where, params);

        } catch (SQLException e) {

            throw new RuntimeException(e);
        }
    }



    @Override
    public Object readColumnsByCondition(Connection conn,
                                         String table,
                                         String cols,
                                         String where,
                                         Object[] params)
            throws SQLException {

        String sql =
                "SELECT " + cols +
                        " FROM " + table +
                        " WHERE " + where;

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            int i=1;

            for (Object p: params) {
                ps.setObject(i++, p);
            }

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getObject(1);
                }
            }
        }

        return null;
    }


    @Override
    public void delete(String table,
                       String where,
                       Object... params) {

        String sql =
                "DELETE FROM " + table +
                        " WHERE " + where;

        try (Connection conn = getDataSource().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            int i=1;

            for (Object p: params) {
                ps.setObject(i++, p);
            }

            ps.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException("Delete failed", e);
        }
    }


    public <R> R withTransactionReturn(
            TransactionCallbackWithReturn<R> cb) {

        try (Connection conn =
                     getDataSource().getConnection()) {

            conn.setAutoCommit(false);

            try {

                R result = cb.execute(conn);
                conn.commit();
                return result;

            } catch (Exception e) {

                conn.rollback();
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {

            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface TransactionCallbackWithReturn<R> {
        R execute(Connection conn) throws Exception;
    }

}