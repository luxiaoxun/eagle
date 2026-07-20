# Eagle Dynamic CEP 设计与使用文档

## 一、概述

Eagle Dynamic CEP 是在 Apache Flink CEP 基础上构建的动态规则引擎，核心能力是**在不重启 Flink 作业的情况下热更新 CEP 规则**。规则以 JSON 格式存储在 MySQL 中，通过 REST API 管理，运行时动态加载和热替换。

### 核心特性

- **规则热更新**：新增、修改、删除规则无需重启 Flink 作业
- **JSON 描述模式**：用 JSON 描述 Pattern DAG，支持节点、边、条件、量词、窗口等完整 Flink CEP 语义
- **多规则并行**：同一事件流可以被多个规则独立匹配，每个规则拥有独立的 NFA 状态
- **增量变更推送**：Coordinator 计算规则差量，只推送变更部分到 TaskManager
- **支持 ProcessingTime / EventTime**
- **规则生命周期管理**：ACTIVE / PAUSE / DRAIN / DELETE

---

## 二、架构

### 2.1 三层解耦

```
┌─────────────────────────────────────────────────────────┐
│                  规则发现层 (Discoverer)                    │
│                                                          │
│  ApiPatternProcessorDiscoverer ──► GET /cep/rules        │
│  (30s 轮询 eagle-api)                                     │
│  JDBCPeriodicPatternProcessorDiscoverer ──► MySQL 直连     │
└──────────────────────┬──────────────────────────────────┘
                       │ onPatternProcessorsUpdated()
                       ▼
┌─────────────────────────────────────────────────────────┐
│                  规则管理层 (Coordinator)                  │
│                                                          │
│  DynamicCepCoordinator (JM 端)                            │
│  ├── 计算增量 delta (added/removed/updated)               │
│  ├── 通过 OperatorEvent 推送到所有 TM                      │
│  └── Checkpoint 规则列表                                   │
└──────────────────────┬──────────────────────────────────┘
                       │ OperatorEvent (RPC)
                       ▼
┌─────────────────────────────────────────────────────────┐
│                  规则消费层 (Operator)                     │
│                                                          │
│  DynamicCepOperator (TM 端)                               │
│  ├── Map<RuleKey, RuleContext> 多规则 NFA 并行              │
│  ├── 每规则独立: ValueState + MapState + TimerService      │
│  ├── 同一事件广播到所有活跃规则                              │
│  └── 生命周期控制: PAUSE/RESUME/DRAIN/DELETE              │
└─────────────────────────────────────────────────────────┘
```

### 2.2 规则数据流

```
eagle-api (管理后台)
  │
  ├── POST /cep/rule    创建/更新规则 → MySQL
  ├── GET  /cep/rules   查询 ACTIVE 规则
  ├── PUT  /cep/rule/{ruleId}/status  变更状态
  └── DELETE /cep/rule/{ruleId}       软删除

Flink Job 启动时:
  1. CepApiIntegrationDemo 同步调用 GET /cep/rules
  2. 将 CepRule 转为 DefaultPatternProcessor 列表
  3. 作为 initialProcessors 传入 ApiPatternProcessorDiscovererFactory
  4. Coordinator 启动 Discoverer 后台轮询

运行时:
  Discoverer (Timer 线程)
    └── 每 30s: GET /cep/rules
        └── 对比 id+version 检测变更
            └── Coordinator 计算 delta
                └── OperatorEvent → DynamicCepOperator
                    └── 热替换 NFA (新增/更新/删除)
```

### 2.3 与 DetectionApp 的关系

Dynamic CEP 与现有的窗口聚合告警（`DynamicAlertFunction`）**互补**，可在同一个 Flink job 中并行运行：

```
Kafka → Transactions
             │
    ┌────────┴────────┐
    ↓                 ↓
DynamicKeyFunction  DynamicCEP.dynamicPatterns()
(窗口聚合)          (序列匹配)
    ↓                 ↓
DynamicAlertFunction DynamicCepOperator
    │                 │
    └────────┬────────┘
             ↓
        Alert Sinks
```

| 场景 | 引擎 | 示例规则 |
|---|---|---|
| 窗口聚合 | DynamicAlertFunction | "用户支付金额一天内 SUM > 100万" |
| 事件序列 | Dynamic CEP | "登录失败3次后5分钟内登录成功" |

---

## 三、规则 JSON 格式

规则使用 `GraphSpec` JSON 描述 Pattern DAG（有向无环图），由节点、边、窗口和跳过策略组成。

### 3.1 顶层结构

```json
{
  "name": "规则名称（英文标识）",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [...],          // Pattern 节点列表
  "edges": [...],          // 节点连接边
  "window": {...},         // 全局时间窗口（可选）
  "afterMatchStrategy": {...}  // 匹配后跳过策略
}
```

### 3.2 Node（节点）

每个节点对应一个 Flink CEP Pattern 步骤。

```json
{
  "name": "step_name",
  "type": "ATOMIC",
  "quantifier": {
    "consumingStrategy": "SKIP_TILL_NEXT",
    "properties": ["SINGLE"],
    "times": null
  },
  "condition": {
    "type": "AVIATOR",
    "expression": "action == 'LOGIN_FAIL'"
  }
}
```

**字段说明**：

| 字段 | 类型 | 说明 |
|---|---|---|
| `name` | String | 节点名称，用于在 `edges` 和输出中引用 |
| `type` | Enum | `ATOMIC` (原子节点) 或 `COMPOSITE` (嵌套子图) |
| `quantifier.consumingStrategy` | Enum | 内部事件选择策略 |
| `quantifier.properties` | Array | 匹配属性列表 |
| `quantifier.times` | Object | 次数约束 (from/to/windowTime) |
| `condition` | Object | 匹配条件 |

### 3.3 Edge（边）

定义节点之间的连接关系。

```json
{
  "source": "step_a",
  "target": "step_b",
  "type": "SKIP_TILL_NEXT"
}
```

**连接策略**（`type`）：

| 策略 | 对应 Flink API | 说明 |
|---|---|---|
| `STRICT` | `pattern.next("b")` | 严格连续，中间不能有其他事件 |
| `SKIP_TILL_NEXT` | `pattern.followedBy("b")` | 宽松连续，中间可跳过不匹配事件（默认） |
| `SKIP_TILL_ANY` | `pattern.followedByAny("b")` | 非确定宽松连续 |

### 3.4 Quantifier（量词）

控制节点匹配的次数和策略。

**`consumingStrategy`（内部事件选择）**：

| 策略 | 对应 Flink API | 说明 |
|---|---|---|
| `STRICT` | `consecutive()` | 匹配的事件之间严格连续 |
| `SKIP_TILL_NEXT` | 默认 | 跳过不匹配事件继续 |
| `SKIP_TILL_ANY` | `allowCombinations()` | 允许组合匹配 |

**`properties`（匹配属性）**：

| 属性 | 对应 Flink API | 说明 |
|---|---|---|
| `SINGLE` | `where(...)` | 单次匹配 |
| `LOOPING` | `timesOrMore(n)` | 循环匹配（配合 times 使用） |
| `TIMES` | `times(n, m)` | 固定次数范围 |
| `OPTIONAL` | `optional()` | 可选匹配 |
| `GREEDY` | `greedy()` | 贪婪匹配 |

**`times`（次数约束）**：

```json
{
  "from": 3,
  "to": 3,
  "windowTime": null
}
```

### 3.5 Condition（条件）

支持 5 种条件类型，通过 Jackson 多态序列化。

#### 3.5.1 AVIATOR — Aviator 表达式（推荐）

```json
{
  "type": "AVIATOR",
  "expression": "action == 'LOGIN_FAIL' && amount > 1000"
}
```

- 表达式在运行时动态编译执行
- 通过 MethodHandle 缓存优化性能（~10-30% 提升）
- ⚠️ **限制**：只能访问当前事件的字段，无法访问 `previous`（迭代上下文）

#### 3.5.2 CLASS — Java 类条件

```json
{
  "type": "CLASS",
  "className": "com.alarm.eagle.cep.condition.EndCondition"
}
```

- 类必须实现 `IterativeCondition<T>`，提供无参构造函数
- 可以访问 `Context.getEventsForPattern()` 获取迭代上下文
- 适合跨事件比较等复杂逻辑

```java
public class CrossEventCondition extends IterativeCondition<UserBehaviorEvent> {
    @Override
    public boolean filter(UserBehaviorEvent event, Context<UserBehaviorEvent> ctx) {
        // 访问前置事件
        for (UserBehaviorEvent prev : ctx.getEventsForPattern("view_product")) {
            if (event.getProduct().equals(prev.getProduct())) {
                return true;
            }
        }
        return false;
    }
}
```

#### 3.5.3 OR / AND / NOT — 复合条件

```json
{
  "type": "OR",
  "conditions": [
    {"type": "AVIATOR", "expression": "action == 'PAYMENT_FAIL' && reason == 'BALANCE_NOT_ENOUGH'"},
    {"type": "AVIATOR", "expression": "action == 'PAYMENT_FAIL' && reason == 'TIMEOUT'"}
  ]
}
```

- 支持任意嵌套
- OR/AND 至少需要 2 个子条件
- NOT 只支持 1 个子条件

### 3.6 Window（全局时间窗口）

所有节点必须在指定时间内完成匹配。

```json
{
  "type": "FIRST_AND_LAST",
  "size": 5,
  "unit": "MINUTES"
}
```

**`type`**：`FIRST_AND_LAST`（从第一个到最后一个事件）或 `PREVIOUS_AND_CURRENT`（从前一个到当前事件）

**`unit`**：`DAYS`, `HOURS`, `MINUTES`, `SECONDS`, `MILLISECONDS`

### 3.7 AfterMatchSkipStrategy（跳过策略）

匹配成功后如何继续匹配。

```json
{
  "type": "SKIP_PAST_LAST_EVENT",
  "patternName": null
}
```

| 策略 | 说明 |
|---|---|
| `NO_SKIP` | 返回所有可能匹配（默认） |
| `SKIP_TO_NEXT` | 从第一个事件之后继续 |
| `SKIP_PAST_LAST_EVENT` | 从匹配的最后一个事件之后继续 |
| `SKIP_TO_FIRST` | 跳到命名模式的第一个事件 |
| `SKIP_TO_LAST` | 跳到命名模式的最后一个事件 |

---

## 四、示例规则

### 4.1 登录暴力破解检测

连续3次登录失败后5分钟内登录成功：

```json
{
  "name": "login_bruteforce",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "login_fail",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "STRICT",
        "properties": ["LOOPING"],
        "times": {"from": 3, "to": 3, "windowTime": null}
      },
      "condition": {"type": "AVIATOR", "expression": "action == 'LOGIN_FAIL'"}
    },
    {
      "name": "login_success",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "action == 'LOGIN_SUCCESS'"}
    }
  ],
  "edges": [
    {"source": "login_fail", "target": "login_success", "type": "SKIP_TILL_NEXT"}
  ],
  "window": {"type": "FIRST_AND_LAST", "size": 5, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "NO_SKIP", "patternName": null}
}
```

**等价 Flink API**：
```java
Pattern.<Event>begin("login_fail")
    .where(new AviatorCondition<>("action == 'LOGIN_FAIL'"))
    .times(3).consecutive()
    .followedBy("login_success")
    .where(new AviatorCondition<>("action == 'LOGIN_SUCCESS'"))
    .within(Time.minutes(5));
```

### 4.2 浏览→下单转化

浏览商品后30分钟内下单：

```json
{
  "name": "view_to_order",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "view_product",
      "type": "ATOMIC",
      "quantifier": {"consumingStrategy": "SKIP_TILL_NEXT", "properties": ["SINGLE"], "times": null},
      "condition": {"type": "AVIATOR", "expression": "action == 'VIEW_PRODUCT'"}
    },
    {
      "name": "place_order",
      "type": "ATOMIC",
      "quantifier": {"consumingStrategy": "SKIP_TILL_NEXT", "properties": ["SINGLE"], "times": null},
      "condition": {"type": "AVIATOR", "expression": "action == 'PLACE_ORDER'"}
    }
  ],
  "edges": [
    {"source": "view_product", "target": "place_order", "type": "SKIP_TILL_NEXT"}
  ],
  "window": {"type": "FIRST_AND_LAST", "size": 30, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "SKIP_PAST_LAST_EVENT", "patternName": null}
}
```

### 4.3 账户盗用检测（多步骤）

登录→修改密码→大额转账，全在40分钟内：

```json
{
  "name": "account_hijack",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "login",
      "type": "ATOMIC",
      "quantifier": {"consumingStrategy": "SKIP_TILL_NEXT", "properties": ["SINGLE"], "times": null},
      "condition": {"type": "AVIATOR", "expression": "action == 'LOGIN_SUCCESS'"}
    },
    {
      "name": "change_pwd",
      "type": "ATOMIC",
      "quantifier": {"consumingStrategy": "SKIP_TILL_NEXT", "properties": ["SINGLE"], "times": null},
      "condition": {"type": "AVIATOR", "expression": "action == 'CHANGE_PASSWORD'"}
    },
    {
      "name": "large_transfer",
      "type": "ATOMIC",
      "quantifier": {"consumingStrategy": "SKIP_TILL_NEXT", "properties": ["SINGLE"], "times": null},
      "condition": {"type": "AVIATOR", "expression": "action == 'TRANSFER' && amount > 50000"}
    }
  ],
  "edges": [
    {"source": "login", "target": "change_pwd", "type": "SKIP_TILL_NEXT"},
    {"source": "change_pwd", "target": "large_transfer", "type": "SKIP_TILL_NEXT"}
  ],
  "window": {"type": "FIRST_AND_LAST", "size": 40, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "NO_SKIP", "patternName": null}
}
```

### 4.4 支付重试检测（OR 条件）

支付失败（余额不足 OR 超时）后2分钟内重试成功：

```json
{
  "name": "payment_retry",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "payment_fail",
      "type": "ATOMIC",
      "quantifier": {"consumingStrategy": "SKIP_TILL_NEXT", "properties": ["SINGLE"], "times": null},
      "condition": {
        "type": "OR",
        "conditions": [
          {"type": "AVIATOR", "expression": "action == 'PAYMENT_FAIL' && reason == 'BALANCE_NOT_ENOUGH'"},
          {"type": "AVIATOR", "expression": "action == 'PAYMENT_FAIL' && reason == 'TIMEOUT'"}
        ]
      }
    },
    {
      "name": "payment_success",
      "type": "ATOMIC",
      "quantifier": {"consumingStrategy": "SKIP_TILL_NEXT", "properties": ["SINGLE"], "times": null},
      "condition": {"type": "AVIATOR", "expression": "action == 'PAYMENT_SUCCESS'"}
    }
  ],
  "edges": [
    {"source": "payment_fail", "target": "payment_success", "type": "SKIP_TILL_NEXT"}
  ],
  "window": {"type": "FIRST_AND_LAST", "size": 2, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "NO_SKIP", "patternName": null}
}
```

---

## 五、API 接口

### 5.1 规则 CRUD

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/cep/rules` | 查询所有 ACTIVE 规则（Flink job 使用） |
| `GET` | `/cep/rules/all` | 查询所有规则（管理后台使用） |
| `GET` | `/cep/rules/hash` | 获取规则 hash（变更检测） |
| `GET` | `/cep/rule/{ruleId}` | 查询单个规则 |
| `POST` | `/cep/rule` | 创建/更新规则 |
| `PUT` | `/cep/rule/{ruleId}/status` | 更新规则状态 |
| `DELETE` | `/cep/rule/{ruleId}` | 删除规则（软删除） |

### 5.2 创建规则

```bash
curl -X POST http://localhost:8080/eagle-api/cep/rule \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "rule_login_bruteforce",
    "name": "登录暴力破解检测",
    "description": "连续3次登录失败后5分钟内登录成功",
    "pattern": "{...GraphSpec JSON...}",
    "function": null,
    "status": 1
  }'
```

- `ruleId`：规则唯一标识，同 id 再次提交时版本号自动递增
- `pattern`：GraphSpec JSON 字符串
- `function`：可选的 `PatternProcessFunction` 全限定类名
- `status`：`1`=ACTIVE, `0`=PAUSED, `-1`=DELETED

### 5.3 更新规则状态

```bash
# 暂停规则
curl -X PUT "http://localhost:8080/eagle-api/cep/rule/rule_login_bruteforce/status?status=0"

# 恢复规则
curl -X PUT "http://localhost:8080/eagle-api/cep/rule/rule_login_bruteforce/status?status=1"

# 删除规则（软删除）
curl -X PUT "http://localhost:8080/eagle-api/cep/rule/rule_login_bruteforce/status?status=-1"
```

---

## 六、集成方式

### 6.1 方式一：initialProcessors 模式（无外部依赖）

```java
// 规则内嵌在代码中，不需要 API 和数据库
List<PatternProcessor<Event>> rules = Arrays.asList(
    new DefaultPatternProcessor<>("rule_1", 1, patternJson,
        new MyOutputFunction("rule_1", 1))
);

PatternProcessorDiscovererFactory<Event> factory =
    new ApiPatternProcessorDiscovererFactory<>(
        "http://localhost:8080/eagle-api",
        rules,    // ← 初始规则
        30000L);

DataStream<String> results = DynamicCEP.dynamicPatterns(
    eventStream, factory, false, TypeInformation.of(String.class));
```

### 6.2 方式二：API 模式（推荐，对应 DetectionApp 模式）

```java
// 启动时同步加载规则，运行时热更新
List<PatternProcessor<Event>> initialRules = fetchRulesFromApi(apiBaseUrl);

PatternProcessorDiscovererFactory<Event> factory =
    new ApiPatternProcessorDiscovererFactory<>(
        apiBaseUrl, initialRules, 30000L);

DataStream<String> results = DynamicCEP.dynamicPatterns(
    eventStream, factory, false, TypeInformation.of(String.class));
```

### 6.3 方式三：JDBC 模式（直连 MySQL）

```java
PatternProcessorDiscovererFactory<Event> factory =
    new JDBCPeriodicPatternProcessorDiscovererFactory<>(
        "jdbc:mysql://localhost:3306/eagle",
        "com.mysql.cj.jdbc.Driver",
        "eagle_cep_rule",
        null,   // 初始规则（可选）
        30000L);
```

### 6.4 PatternProcessFunction 输出函数

规则匹配后的处理逻辑，需实现 `PatternProcessFunction<T, OUT>`：

```java
public class AlertOutputFunction
        extends PatternProcessFunction<UserBehaviorEvent, String> {

    private final String ruleId;
    private final int version;

    // 必须提供 (String, int) 构造函数（用于反射实例化）
    public AlertOutputFunction(String ruleId, int version) {
        this.ruleId = ruleId;
        this.version = version;
    }

    @Override
    public void processMatch(
            Map<String, List<UserBehaviorEvent>> match,
            Context ctx, Collector<String> out) {
        // match key 对应节点 name，value 为匹配到的事件列表
        List<UserBehaviorEvent> loginFails = match.get("login_fail");
        List<UserBehaviorEvent> loginSuccess = match.get("login_success");

        String alert = String.format(
            "[ALERT] Rule %s(v%d): user=%s, failCount=%d",
            ruleId, version,
            loginFails.get(0).getUserId(),
            loginFails.size());
        out.collect(alert);
    }
}
```

---

## 七、模块文件清单

### 7.1 eagle-detection/cep/

```
cep/
├── DynamicCEP.java                       # API 入口
├── PatternWrapper.java                   # Pattern protected 构造器暴露
├── condition/
│   └── AviatorCondition.java             # Aviator 动态条件（MethodHandle 缓存）
├── json/
│   ├── CepJsonUtils.java                 # JSON ↔ Pattern 双向转换
│   ├── TimeDeserializer.java
│   └── spec/
│       ├── ConditionSpec.java            # 条件多态基类
│       ├── AviatorConditionSpec.java
│       ├── ClassConditionSpec.java
│       ├── RichOrConditionSpec.java
│       ├── RichAndConditionSpec.java
│       ├── RichNotConditionSpec.java
│       ├── NodeSpec.java                 # Pattern 节点
│       ├── EdgeSpec.java                 # Pattern 边
│       ├── GraphSpec.java                # Pattern DAG 完整图
│       ├── QuantifierSpec.java
│       ├── TimesSpec.java
│       ├── WindowSpec.java
│       └── AfterMatchSkipStrategySpec.java
├── operator/
│   ├── DynamicCepOperator.java           # 多 NFA 并行引擎
│   ├── DynamicCepOperatorFactory.java    # CoordinatedOperatorFactory
│   ├── DynamicCepCoordinator.java        # JM 端规则协调器
│   ├── DynamicCepCoordinatorProvider.java
│   ├── PatternProcessorChangeEvent.java  # 增量变更事件
│   └── RuleLifecycleEvent.java           # 生命周期事件
└── processor/
    ├── PatternProcessor.java             # 规则接口
    ├── DefaultPatternProcessor.java      # JSON 存储实现
    ├── PatternProcessorDiscoverer.java
    ├── PatternProcessorDiscovererFactory.java
    ├── PatternProcessorManager.java
    ├── PeriodicPatternProcessorDiscoverer.java  # Timer 轮询基类
    ├── ApiPatternProcessorDiscoverer.java       # HTTP API 实现
    ├── ApiPatternProcessorDiscovererFactory.java
    ├── JDBCPeriodicPatternProcessorDiscoverer.java  # MySQL 直连实现
    ├── JDBCPeriodicPatternProcessorDiscovererFactory.java
    └── RuleState.java
```

### 7.2 eagle-api（规则管理）

```
eagle-api/
├── database/
│   └── db_cep_rule.sql                   # 建表 + 示例数据
└── src/main/java/.../api/
    ├── dao/CepRuleDo.java                # JPA Entity
    ├── dao/repository/CepRuleRepository.java
    ├── service/CepRuleService.java       # 业务逻辑
    └── controller/CepRuleController.java # REST 接口
```

### 7.3 eagle-common

```
eagle-common/src/main/java/.../model/
└── CepRule.java                          # 规则 Model
```

---

## 八、注意事项

1. **Aviator 条件限制**：`SimpleCondition` 只能访问当前事件字段，无法访问 `previous`。跨事件比较需使用 CLASS 类型实现 `IterativeCondition`。

2. **规则版本管理**：同 `ruleId` 提交新规则时，`version` 自动递增。Discoverer 检测到版本变化后只推送变更部分。

3. **状态清理**：规则下线时旧版本的状态通过 deferred cleanup 机制清理，但不是立即的（依赖 State TTL）。

4. **SharedBuffer 共享**：当前实现中所有规则共享一个 `SharedBuffer`，由于使用不同的 state name，没有实际冲突。

5. **Flink 版本**：需要 Flink 1.17.x，使用 `NFACompiler`/`SharedBuffer`/`OperatorCoordinator` 等 API。

6. **依赖**：需要添加 `aviator:5.3.1` 和 `jackson-databind` 依赖。
