package com.alarm.eagle.cep.processor;

import com.alarm.eagle.cep.json.CepJsonUtils;
import org.apache.flink.cep.functions.PatternProcessFunction;
import org.apache.flink.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JDBC periodic rule discoverer.
 *
 * <p>Periodically pulls the latest rules from a database table via JDBC. Detection logic:
 * <ol>
 *   <li>Full SELECT * FROM tableName</li>
 *   <li>For same id, keep the record with the maximum version</li>
 *   <li>Compare old vs new Map to detect changes</li>
 *   <li>On change, construct DefaultPatternProcessor list and callback</li>
 * </ol>
 *
 * <p><b>Database table schema</b>:
 * <pre>
 *   id       VARCHAR  — unique rule identifier
 *   version  INT      — rule version number (incrementing)
 *   pattern  TEXT     — Pattern JSON (GraphSpec format)
 *   function VARCHAR  — PatternProcessFunction fully qualified class name (optional)
 * </pre>
 *
 * @param <T> event type
 */
public class JDBCPeriodicPatternProcessorDiscoverer<T>
        extends PeriodicPatternProcessorDiscoverer<T> {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    JDBCPeriodicPatternProcessorDiscoverer.class);

    private final String tableName;
    private final String jdbcUrl;
    private final ClassLoader userCodeClassLoader;
    private final List<PatternProcessor<T>> initialProcessors;

    private Connection connection;
    private Statement statement;
    /** ★ cached last query result, (id → (id, version, pattern, function)) */
    private Map<String, RowData> latestData;

    /**
     * @param jdbcUrl               JDBC URL
     * @param jdbcDriver             JDBC driver class name
     * @param tableName              rule table name
     * @param userCodeClassLoader    user code ClassLoader
     * @param initialProcessors      initial rules (used when database is empty), nullable
     * @param intervalMillis         polling interval in milliseconds
     */
    public JDBCPeriodicPatternProcessorDiscoverer(
            String jdbcUrl,
            String jdbcDriver,
            String tableName,
            ClassLoader userCodeClassLoader,
            @Nullable List<PatternProcessor<T>> initialProcessors,
            @Nullable Long intervalMillis) throws Exception {
        super(intervalMillis);
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
        this.tableName = Objects.requireNonNull(tableName);
        this.userCodeClassLoader = userCodeClassLoader;
        this.initialProcessors = initialProcessors;

        // Initialize JDBC connection (defer retry to first query if connection fails)
        Class.forName(Objects.requireNonNull(jdbcDriver));
        try {
            this.connection = DriverManager.getConnection(jdbcUrl);
            this.statement = connection.createStatement();
            LOG.info("[DISCOVER] JDBC connection successful: " + jdbcUrl);
        } catch (SQLException e) {
            // ★ Connection failure does not block Discoverer creation (allows initialProcessors mode)
            this.connection = null;
            this.statement = null;
            LOG.info("[DISCOVER] JDBC connection failed (will use initialProcessors mode): "
                    + e.getMessage());
        }
    }

    // ======================== Change Detection ========================

    @Override
    public boolean arePatternProcessorsUpdated() {
        // First run with initial rules
        if (latestData == null
                && initialProcessors != null
                && !initialProcessors.isEmpty()) {
            LOG.info("[DISCOVER] Using initial rules (total: "
                    + initialProcessors.size() + ")");
            return true;
        }

        if (statement == null) {
            LOG.info("[DISCOVER] ⚠ statement is null, skipping check");
            return false;
        }

        try {
            LOG.info("[DISCOVER] Querying MySQL: SELECT * FROM " + tableName + " ...");
            ResultSet rs = statement.executeQuery(
                    "SELECT * FROM " + tableName);
            Map<String, RowData> current = new HashMap<>();

            while (rs.next()) {
                String id = rs.getString("id");
                int version = rs.getInt("version");
                // Multiple versions for same id → keep only the one with max version
                RowData existing = current.get(id);
                if (existing != null && existing.version >= version) {
                    continue;
                }
                current.put(id, new RowData(
                        id,
                        version,
                        rs.getString("pattern"),
                        rs.getString("function")));
            }

            if (latestData == null || isChanged(current)) {
                LOG.info("[DISCOVER] ✅ Rule change detected (" + current.size() + ")");
                latestData = current;
                return true;
            }
            LOG.info("[DISCOVER] No changes (" + current.size() + " rules)");
            return false;

        } catch (SQLException e) {
            LOG.warn("Failed to check rule changes, reconnecting...", e);
            LOG.info("[DISCOVER] ⚠ MySQL query failed: " + e.getMessage() + ", attempting reconnect...");
            reconnect();
            return false;
        }
    }

    // ======================== Get Latest Rules ========================

    @Override
    @SuppressWarnings("unchecked")
    public List<PatternProcessor<T>> getLatestPatternProcessors()
            throws Exception {
        // ★ initialProcessors mode: return directly when latestData is null but initial rules exist
        if (latestData == null && initialProcessors != null
                && !initialProcessors.isEmpty()) {
            // Set latestData to empty Map to avoid repeated triggers in subsequent polls
            latestData = new HashMap<>();
            LOG.info("[DISCOVER] Returning initial rules (total: "
                    + initialProcessors.size() + ")");
            return new ArrayList<>(initialProcessors);
        }
        if (latestData == null) {
            throw new IllegalStateException(
                    "latestData is null. Call arePatternProcessorsUpdated() first.");
        }

        return latestData.values().stream()
                .map(row -> {
                    try {
                        String id = row.id;
                        int version = row.version;
                        String patternStr = row.pattern;

                        // reflective loading of PatternProcessFunction (if configured)
                        PatternProcessFunction<T, ?> func = null;
                        if (!StringUtils.isNullOrWhitespaceOnly(row.function)) {
                            func = (PatternProcessFunction<T, ?>)
                                    userCodeClassLoader
                                            .loadClass(row.function)
                                            .getConstructor(String.class, int.class)
                                            .newInstance(id, version);
                        }

                        return new DefaultPatternProcessor<>(
                                id, version, patternStr, func);
                    } catch (Exception e) {
                        LOG.error("Failed to create PatternProcessor for {}: {}",
                                row.id, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ======================== Helper Methods ========================

    private boolean isChanged(Map<String, RowData> current) {
        if (latestData.size() != current.size()) {
            return true;
        }
        return !current.equals(latestData);
    }

    private void reconnect() {
        try {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
            connection = DriverManager.getConnection(jdbcUrl);
            statement = connection.createStatement();
        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Cannot reconnect to database: " + jdbcUrl, ex);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            LOG.warn("Failed to close statement.", e);
        } finally {
            statement = null;
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            LOG.warn("Failed to close connection.", e);
        } finally {
            connection = null;
        }
    }

    // ======================== Inner Classes ========================

    /** A single database row */
    private static class RowData {
        final String id;
        final int version;
        final String pattern;
        final String function;

        RowData(String id, int version, String pattern, String function) {
            this.id = id;
            this.version = version;
            this.pattern = pattern;
            this.function = function;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RowData)) return false;
            RowData r = (RowData) o;
            return version == r.version
                    && Objects.equals(id, r.id)
                    && Objects.equals(pattern, r.pattern)
                    && Objects.equals(function, r.function);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, version, pattern, function);
        }
    }
}
