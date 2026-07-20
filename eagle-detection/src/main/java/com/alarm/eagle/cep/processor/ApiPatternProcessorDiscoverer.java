package com.alarm.eagle.cep.processor;

import com.alarm.eagle.cep.json.CepJsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ★ HTTP-based rule discoverer — polls eagle-api for CEP rules.
 *
 * <p>Replaces {@link JDBCPeriodicPatternProcessorDiscoverer} with HTTP polling,
 * fetching rules from eagle-api's {@code GET /cep/rules} endpoint.
 *
 * <p>Rule JSON format from API:
 * <pre>{@code
 * [
 *   {
 *     "id": "rule_1",
 *     "version": 1,
 *     "pattern": "{...GraphSpec JSON...}",
 *     "function": null
 *   }
 * ]
 * }</pre>
 *
 * @param <T> event type
 */
public class ApiPatternProcessorDiscoverer<T>
        extends PeriodicPatternProcessorDiscoverer<T> {

    private static final Logger LOG =
            LoggerFactory.getLogger(ApiPatternProcessorDiscoverer.class);

    private final String apiBaseUrl;
    private final ClassLoader userCodeClassLoader;
    private final List<PatternProcessor<T>> initialProcessors;
    private List<PatternProcessor<T>> latestProcessors;

    private static final ObjectMapper mapper = CepJsonUtils.getObjectMapper();

    /**
     * @param apiBaseUrl           eagle-api 基础 URL
     * @param userCodeClassLoader  ClassLoader
     * @param initialProcessors    初始规则
     * @param intervalMillis       轮询间隔
     */
    public ApiPatternProcessorDiscoverer(
            String apiBaseUrl,
            ClassLoader userCodeClassLoader,
            @Nullable List<PatternProcessor<T>> initialProcessors,
            Long intervalMillis) {
        super(intervalMillis);
        this.apiBaseUrl = Objects.requireNonNull(apiBaseUrl);
        this.userCodeClassLoader = userCodeClassLoader;
        this.initialProcessors = initialProcessors;
    }

    @Override
    public boolean arePatternProcessorsUpdated() {
        // First run: use initial rules if available
        if (latestProcessors == null
                && initialProcessors != null
                && !initialProcessors.isEmpty()) {
            System.out.println("[API-DISCOVER] Using initial rules (total: "
                    + initialProcessors.size() + ")");
            return true;
        }

        try {
            String rulesUrl = apiBaseUrl + "/cep/rules";
            System.out.println("[API-DISCOVER] Polling: GET " + rulesUrl);
            String json = httpGet(rulesUrl);
            if (json == null || json.isEmpty()) {
                System.out.println("[API-DISCOVER] ⚠ Empty response from API");
                return false;
            }

            JsonNode root = mapper.readTree(json);
            // API 返回 Response 包装: { "code": 0, "data": [...] }
            JsonNode dataNode = root.has("data") ? root.get("data") : root;

            List<PatternProcessor<T>> current = new ArrayList<>();
            if (dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    String ruleId = item.get("ruleId").asText();
                    int version = item.get("version").asInt();
                    String patternStr = item.get("pattern").asText();
                    String functionClassName = item.has("function")
                            && !item.get("function").isNull()
                            ? item.get("function").asText() : null;

                    DefaultPatternProcessor<T> pp = new DefaultPatternProcessor<>(
                            ruleId, version, patternStr, null);
                    current.add(pp);
                }
            }

            if (latestProcessors == null || isChanged(current)) {
                System.out.println("[API-DISCOVER] ✅ Rule change detected (" +
                        current.size() + " rules)");
                latestProcessors = current;
                return true;
            }
            System.out.println("[API-DISCOVER] No changes (" +
                    current.size() + " rules)");
            return false;

        } catch (Exception e) {
            LOG.warn("Failed to check rule changes from API: {}", e.getMessage());
            System.out.println("[API-DISCOVER] ⚠ API query failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PatternProcessor<T>> getLatestPatternProcessors()
            throws Exception {
        if (latestProcessors == null && initialProcessors != null
                && !initialProcessors.isEmpty()) {
            latestProcessors = new ArrayList<>();
            System.out.println("[API-DISCOVER] Returning initial rules (total: "
                    + initialProcessors.size() + ")");
            return new ArrayList<>(initialProcessors);
        }
        if (latestProcessors == null) {
            throw new IllegalStateException(
                    "latestProcessors is null. Call arePatternProcessorsUpdated() first.");
        }
        return new ArrayList<>(latestProcessors);
    }

    private boolean isChanged(List<PatternProcessor<T>> current) {
        if (latestProcessors == null) return true;
        if (latestProcessors.size() != current.size()) return true;

        java.util.Map<String, Integer> oldMap = latestProcessors.stream()
                .collect(Collectors.toMap(
                        PatternProcessor::getId, PatternProcessor::getVersion));
        java.util.Map<String, Integer> newMap = current.stream()
                .collect(Collectors.toMap(
                        PatternProcessor::getId, PatternProcessor::getVersion));
        return !oldMap.equals(newMap);
    }

    private String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code != 200) {
                System.out.println("[API-DISCOVER] HTTP " + code + " from " + urlStr);
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
