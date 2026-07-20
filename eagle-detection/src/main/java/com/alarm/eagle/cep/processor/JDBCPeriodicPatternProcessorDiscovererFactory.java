package com.alarm.eagle.cep.processor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Factory class for JDBCPeriodicPatternProcessorDiscoverer.
 *
 * <p>Holds JDBC connection parameters; creates a new Discoverer instance in
 * {@code createPatternProcessorDiscoverer()}.
 *
 * @param <T> event type
 */
public class JDBCPeriodicPatternProcessorDiscovererFactory<T>
        implements PatternProcessorDiscovererFactory<T> {

    private static final long serialVersionUID = 1L;

    private final String jdbcUrl;
    private final String jdbcDriver;
    private final String tableName;
    private final List<PatternProcessor<T>> initialProcessors;
    private final Long intervalMillis;

    /**
     * @param jdbcUrl            JDBC URL
     * @param jdbcDriver          JDBC driver class name
     * @param tableName           rule table name
     * @param initialProcessors   Initial rules (may be null)
     * @param intervalMillis      polling interval in milliseconds
     */
    public JDBCPeriodicPatternProcessorDiscovererFactory(
            String jdbcUrl,
            String jdbcDriver,
            String tableName,
            @Nullable List<PatternProcessor<T>> initialProcessors,
            Long intervalMillis) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
        this.jdbcDriver = Objects.requireNonNull(jdbcDriver);
        this.tableName = Objects.requireNonNull(tableName);
        this.initialProcessors = initialProcessors;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public PatternProcessorDiscoverer<T> createPatternProcessorDiscoverer(
            ClassLoader userCodeClassLoader) throws Exception {
        return new JDBCPeriodicPatternProcessorDiscoverer<>(
                jdbcUrl, jdbcDriver, tableName,
                userCodeClassLoader,
                initialProcessors,
                intervalMillis);
    }
}
