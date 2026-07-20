# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the full project (tests skipped by default via <skipTests>true</skipTests>)
mvn clean package -Dmaven.test.skip=true

# Build a specific module with dependencies
mvn -pl eagle-api -am package -Dmaven.test.skip=true -Pdev
mvn -pl eagle-detection -am package -Dmaven.test.skip=true
mvn -pl eagle-alert -am package -Dmaven.test.skip=true
mvn -pl eagle-log -am package -Dmaven.test.skip=true

# Run tests for a specific module (override skipTests)
mvn -pl eagle-api test -DskipTests=false
mvn -pl eagle-common test -DskipTests=false

# Run a single test class
mvn -pl eagle-alert test -DskipTests=false -Dtest=SiddhiTest
mvn -pl eagle-api test -DskipTests=false -Dtest=NotifyTest
```

The Flink job modules (`eagle-detection`, `eagle-alert`, `eagle-log`) use the `maven-shade-plugin` to produce fat JARs. The API module (`eagle-api`) uses `spring-boot-maven-plugin` for a repackaged boot JAR.

A convenience script `eagle-api/build.sh` builds the API module and assembles an output directory with the JAR, control script, and environment config.

## Architecture

Eagle is a **real-time alarm detection platform** using Apache Flink with three different CEP/rule engines. It has 5 Maven modules (Java 8, Flink 1.16.3, Spring Boot 2.5.12).

### Module Map

| Module | Type | Role |
|---|---|---|
| `eagle-common` | Library | Shared models, utilities, constants — no Flink deps |
| `eagle-api` | Spring Boot app | Central REST API for rule/task CRUD, alert notification dispatch |
| `eagle-alert` | Flink job | Siddhi CEP engine for policy-based data alerting |
| `eagle-detection` | Flink job | Dynamic rule evaluation with broadcast state (fraud detection) |
| `eagle-log` | Flink job | Drools rule engine for log processing → ES + Redis |

All Flink/API modules depend on `eagle-common`. There are no other inter-module dependencies.

### Entry Points (4 `main()` classes)

1. **`EagleApiApp`** — Spring Boot REST API on port 9080 (typical). Serves task/policy/rule configs to the Flink jobs, receives alert results, dispatches notifications (mail/WeChat/SMS/HTTP callback).

2. **`EagleLogApp`** — `eagle-log` Flink job. Kafka → `LogProcessFunction` (Drools rules via broadcast state) → Elasticsearch + Redis aggregation.

3. **`DetectionApp`** — `eagle-detection` Flink job. Kafka/Generator → `DynamicKeyFunction` (broadcast-state-driven keying) → `DynamicAlertFunction` (windowed aggregation + threshold comparison) → Alert sinks.

4. **`EagleAlertApp`** — `eagle-alert` Flink job. Takes `taskId` arg, fetches Task config from eagle-api, unions multiple Kafka sources → `SiddhiOperator` (Siddhi CEP runtime) → `ApiSink` (HTTP POST back to eagle-api).

### Runtime Data Flow

```
eagle-api (MySQL/JPA) ←──config queries── eagle-alert, eagle-log, eagle-detection
     ↑                                              │
     └──────────── alert results ───────────────────┘
                         │
                         ├── Mail / WeChat / SMS / HTTP callback
                         └── (via NotifyService with cooldown logic)
```

- `eagle-log` polls `GET /log/rules` for Drools rule updates
- `eagle-alert` calls `GET /task/query?taskId=` for Siddhi task config, POSTs results to `/data/sink`
- `eagle-detection` reads rules from Kafka (via `RulesSource`) rather than HTTP

### Key Architecture Patterns

**Broadcast State (eagle-detection)**: `DynamicKeyFunction` and `DynamicAlertFunction` form a two-stage broadcast pipeline. Rules are broadcast to all parallel subtasks. `DynamicKeyFunction` keys each event by rule's `groupingKeyNames`, then `DynamicAlertFunction` maintains per-key window state and aggregates values (SUM/AVG/MIN/MAX) over sliding time windows, comparing against rule thresholds.

**Siddhi Operator Lifecycle (eagle-alert)**: `SiddhiOperator` (a Flink `AbstractStreamOperator`) wraps WSO2 Siddhi runtime. Uses a priority queue for event-time ordering. `SiddhiTaskManager` hot-swaps Siddhi runtimes when task config changes (detected via background polling thread). Checkpoints Siddhi runtime snapshots using Flink managed operator state with union redistribution.

**Drools Rule Processing (eagle-log)**: `LogProcessFunction` loads Drools rules into a `KieSession`. On broadcast state update (new rules from API), it destroys and recreates the session if the rule hash differs.

**Notification Chain (eagle-api)**: `NotifyFactory` selects `Notify` strategy by `AlertType`. `NotifyService` runs an async thread pool (2-4 threads, 1000-capacity queue, `CallerRunsPolicy`) with per-policy cooldown to prevent alert storms.

### CEP Engines Used

| Module | Engine | Purpose |
|---|---|---|
| `eagle-detection` | Flink CEP + custom accumulators | Transaction fraud detection with dynamic rules |
| `eagle-alert` | WSO2 Siddhi 4.0.0 | CQL-based policy evaluation over unioned streams |
| `eagle-log` | Drools 7.74.1 | Rule-based log filtering, enrichment, and classification |

### Dynamic CEP Module (`eagle-detection/.../cep/`)

The `eagle-detection` module now includes a full Dynamic CEP framework (33 Java files) ported from Flink-Dynamic-CEP-FP. It enables runtime hot-swapping of Flink CEP patterns without job restart.

**Package structure:**
- `cep/` — `DynamicCEP.java` API entry point, `PatternWrapper.java`
- `cep/condition/` — `AviatorCondition.java` (Aviator expression engine for dynamic conditions)
- `cep/json/` — `CepJsonUtils.java` (JSON ↔ Pattern bidirectional conversion), `TimeDeserializer.java`
- `cep/json/spec/` — 12 spec classes for JSON serialization of Pattern DAG (`GraphSpec`, `NodeSpec`, `EdgeSpec`, `ConditionSpec` + subtypes, `QuantifierSpec`, `TimesSpec`, `WindowSpec`, `AfterMatchSkipStrategySpec`)
- `cep/operator/` — `DynamicCepOperator.java` (multi-NFA engine with per-rule state isolation), `DynamicCepOperatorFactory.java`, `DynamicCepCoordinator.java` (JM-side rule distribution), `PatternProcessorChangeEvent.java`, `RuleLifecycleEvent.java`
- `cep/processor/` — Rule abstraction (`PatternProcessor`, `DefaultPatternProcessor`), discovery interfaces (`PatternProcessorDiscoverer`, `PatternProcessorDiscovererFactory`, `PatternProcessorManager`), `PeriodicPatternProcessorDiscoverer.java`, `JDBCPeriodicPatternProcessorDiscoverer.java` (MySQL-backed rule store)

**Key capabilities:**
- Rules stored as JSON in MySQL or passed programmatically via `initialProcessors`
- CEP patterns support Aviator expressions, Java class conditions, and compound OR/AND/NOT conditions
- Multi-rule parallel NFA evaluation — each event evaluated against all active rules
- Incremental rule hot-swap (add/remove/update) via Flink OperatorEvent
- Rule lifecycle: PAUSE/RESUME/DRAIN/DELETE
- Per-rule isolated Flink state (ValueState, MapState, TimerService, Metrics)
- JM-side Coordinator with checkpoint support

**Usage in DetectionApp:**
```java
import com.alarm.eagle.cep.DynamicCEP;
SingleOutputStreamOperator<String> cepResults = DynamicCEP.dynamicPatterns(
    transactions,
    new JDBCPeriodicPatternProcessorDiscovererFactory<>(
        jdbcUrl, jdbcDriver, tableName, null, intervalMs),
    false,  // isProcessingTime = EventTime
    TypeInformation.of(String.class));
```

**New dependency:** `com.googlecode.aviator:aviator:5.3.1`

## Configuration

- **Profiles**: `dev` (default) and `prod`, activated via `-Pdev` or `-Pprod`
- **eagle-api config**: `application-{profile}.properties` — MySQL, Kafka, ES, mail/SMS/WeChat settings
- **eagle-alert config**: `application-{profile}.properties` + `mode=dev|prod`
- **eagle-log config**: `conf.properties` — Kafka, ES, Redis, rule URL (defaults to `http://localhost:9080/eagle-api/log/rules`)
- **eagle-detection config**: Passed via Flink `ParameterTool` at job submission
- **Logging**: All modules use Log4j2 (not Logback). Each has its own `log4j2.xml` with console + rolling file appenders.

## Notable Caveats

- **Flink 1.17.2**: Upgraded from 1.16.3 for Dynamic CEP compatibility. `TimeCharacteristic` and `BoundedOutOfOrdernessTimestampExtractor` have been replaced with `WatermarkStrategy`. Old `FlinkKafkaConsumer`/`FlinkKafkaProducer` are deprecated but still functional.
- Tests are **globally skipped** in the root POM (`<skipTests>true</skipTests>`). Must explicitly pass `-DskipTests=false` to run them.
- `eagle-detection` has **no test dependencies and no test classes**.
- WSO2 Maven repositories are required for Siddhi artifacts — configured in root POM. Internal Tencent mirror is the primary repo.
- Flink dependencies (`flink-streaming-java`, `flink-cep`, `flink-cep-scala`) are set to `<scope>provided</scope>` in the root POM — they are expected on the Flink cluster classpath.
- Eagle uses **Spring Boot's Log4j2 starter** and explicitly excludes `spring-boot-starter-logging` to avoid Logback conflicts.
