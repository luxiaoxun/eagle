package com.alarm.eagle.cep.processor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Factory for ApiPatternProcessorDiscoverer — HTTP-based rule discovery from eagle-api.
 *
 * <p>Holds the API base URL and polling configuration.
 *
 * @param <T> event type
 */
public class ApiPatternProcessorDiscovererFactory<T>
        implements PatternProcessorDiscovererFactory<T> {

    private static final long serialVersionUID = 1L;

    private final String apiBaseUrl;
    private final List<PatternProcessor<T>> initialProcessors;
    private final Long intervalMillis;

    /**
     * @param apiBaseUrl         eagle-api 基础 URL, e.g. "http://localhost:9080/eagle-api"
     * @param initialProcessors  初始规则 (可为 null)
     * @param intervalMillis     轮询间隔（毫秒），推荐 10000-60000
     */
    public ApiPatternProcessorDiscovererFactory(
            String apiBaseUrl,
            @Nullable List<PatternProcessor<T>> initialProcessors,
            Long intervalMillis) {
        this.apiBaseUrl = Objects.requireNonNull(apiBaseUrl);
        this.initialProcessors = initialProcessors;
        this.intervalMillis = intervalMillis != null ? intervalMillis : 30000L;
    }

    @Override
    public PatternProcessorDiscoverer<T> createPatternProcessorDiscoverer(
            ClassLoader userCodeClassLoader) throws Exception {
        return new ApiPatternProcessorDiscoverer<>(
                apiBaseUrl, userCodeClassLoader,
                initialProcessors, intervalMillis);
    }
}
