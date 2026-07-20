# Eagle Dynamic CEP 设计与使用文档

## 一、概述

Eagle Dynamic CEP 是基于 Apache Flink CEP 构建的动态规则引擎，核心能力是**在不重启 Flink 作业的情况下热更新 CEP 规则**。规则以 JSON 格式存储在 MySQL 中，通过 REST API 管理，运行时动态加载和热替换。

`eagle-detection` 模块已统一使用 Dynamic CEP 作为事件序列匹配引擎。

### 核心特性

- **规则热更新**：新增、修改、删除规则无需重启 Flink 作业
- **JSON 描述模式**：用 JSON 描述 Pattern DAG，支持节点、边、条件、量词、窗口等完整 Flink CEP 语义
- **多规则并行**：同一事件流可以被多个规则独立匹配，每个规则拥有独立的 NFA 状态
- **增量变更推送**：Coordinator 计算规则差量，只推送变更部分到 TaskManager
- **Aviator 动态条件**：条件以字符串表达式存储（如 `paymentAmount > 50000`），运行时动态编译执行
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

### 2.2 DetectionApp 数据流

```
eagle-api (管理后台)                    eagle-detection (Flink Job)
  │                                        │
  ├── POST /cep/rule  创建规则 → MySQL      │
  ├── POST /mock/transaction/batch         │ Kafka (eagle-transaction)
  │    生成随机交易 → Kafka ─────────────────┼──► KafkaSource
  │                                        │       │
  │                                        │       ▼
  │                                        │   Transaction.fromString()
  │                                        │       │
  │                                        │       ▼
  │   Startup: 同步加载                      │   WatermarkStrategy
  │   GET /cep/rules ←─────────────────────┤       │
  │   返回 List<CepRule>                     │       ▼
  │   → initialProcessors                   │   DynamicCEP.dynamicPatterns()
  │                                        │       │
  │   Runtime: 30s 轮询                      │       ▼
  │   ApiPatternProcessorDiscoverer ────────┤   DynamicCepOperator
  │   检测变更 → OperatorEvent               │   (多 NFA 并行匹配)
  │                                        │       │
  │                                        │       ▼
  │                                        │   CEP-ALERT 输出
```

---

## 三、模块说明

### 3.1 eagle-detection

```
eagle-detection/src/main/java/com/alarm/eagle/
├── DetectionApp.java              # ★ 入口：Kafka → Dynamic CEP
├── config/
│   ├── Constant.java              # 配置常量
│   └── DetectionProperties.java   # 参数管理（dev/prod）
├── message/
│   └── Transaction.java           # 交易事件模型（CSV 解析）
└── cep/                           # Dynamic CEP 引擎（33 文件）
    ├── DynamicCEP.java            # API 入口
    ├── PatternWrapper.java
    ├── condition/
    │   └── AviatorCondition.java
    ├── json/
    │   ├── CepJsonUtils.java
    │   ├── TimeDeserializer.java
    │   └── spec/ (12 个 JSON 序列化类)
    ├── operator/
    │   ├── DynamicCepOperator.java
    │   ├── DynamicCepOperatorFactory.java
    │   ├── DynamicCepCoordinator.java
    │   ├── DynamicCepCoordinatorProvider.java
    │   ├── PatternProcessorChangeEvent.java
    │   └── RuleLifecycleEvent.java
    └── processor/
        ├── PatternProcessor.java
        ├── DefaultPatternProcessor.java
        ├── ApiPatternProcessorDiscoverer.java
        ├── ApiPatternProcessorDiscovererFactory.java
        ├── JDBCPeriodicPatternProcessorDiscoverer.java
        ├── JDBCPeriodicPatternProcessorDiscovererFactory.java
        ├── PeriodicPatternProcessorDiscoverer.java
        └── RuleState.java
```

### 3.2 事件模型 — Transaction

```java
public class Transaction implements Serializable {
    long transactionId;      // 交易 ID
    long eventTime;          // 事件时间 (ms)
    long payerId;            // 付款方 ID
    long beneficiaryId;      // 收款方 ID
    String paymentType;      // CSH / CRD
    BigDecimal paymentAmount;// 金额
    long ingestionTimestamp; // 摄入时间戳

    // CSV 解析: transactionId,eventTime,payerId,beneficiaryId,paymentType,paymentAmount,ingestionTimestamp
    public static Transaction fromString(String csvLine) { ... }
}
```

Aviator 表达式中可直接使用 getter 名：`payerId`, `beneficiaryId`, `paymentAmount`, `paymentType`, `transactionId`, `eventTime`

### 3.3 eagle-api

```
eagle-api/src/main/java/com/alarm/eagle/api/
├── dao/
│   ├── CepRuleDo.java                # JPA Entity
│   └── repository/CepRuleRepository.java
├── service/
│   ├── CepRuleService.java           # 规则 CRUD 业务逻辑
│   └── mock/
│       └── TransactionDataMockService.java  # ★ 交易数据 Mock
├── controller/
│   ├── CepRuleController.java        # 规则管理接口
│   └── DataMockController.java       # Mock 数据接口
└── database/
    └── db_cep_rule.sql               # 建表 + 7 条示例规则
```

---

## 四、DetectionApp 配置与运行

### 4.1 配置参数

| 参数 | dev 默认值 | 说明 |
|---|---|---|
| `mode` | `dev` | 运行模式 |
| `api.base.url` | `http://localhost:8080/eagle-api` | 规则 API 地址 |
| `cep.rule.poll.interval.ms` | `30000` | 规则轮询间隔 |
| `kafka.bootstrap.servers` | `localhost:9092` | Kafka 地址 |
| `kafka.topic` | `eagle-transaction` | 消费 Topic |
| `kafka.topic.parallelism` | `1` | 消费并行度 |
| `transaction.out.of.orderness` | `60000` | 事件乱序容忍度(ms) |
| `flink.parallelism` | `2` | Flink 全局并行度 |
| `flink.enable.checkpoint` | `true` | 启用 Checkpoint |

### 4.2 运行步骤

**1. 初始化数据库**
```bash
mysql -u root -p eagle < eagle-api/database/db_cep_rule.sql
```

**2. 启动 eagle-api**
```bash
cd eagle-api && mvn spring-boot:run -Pdev
# 访问 http://localhost:8080/eagle-api/swagger-ui.html
```

**3. 启动 eagle-detection**
```bash
cd eagle-detection && mvn exec:java -Dexec.mainClass="com.alarm.eagle.DetectionApp" -Pdev
```

**4. 发送 Mock 交易数据**
```bash
# 5 线程，每线程 100 条，间隔 100ms，20% 大额交易
curl -X POST "http://localhost:8080/eagle-api/mock/transaction/batch?threads=5&count=100&intervalMs=100&largePercent=20"

# 触发小额→大额配对规则
curl -X POST "http://localhost:8080/eagle-api/mock/transaction/small-then-large?payerId=1"
```

**5. 观察输出**
```bash
tail -f logs/eagle-detection.log | grep "CEP-ALERT"
```

---

## 五、Mock 数据服务

### 5.1 TransactionDataMockService

仅 `dev` 环境可用，发送随机生成的交易 CSV 数据到 `eagle-transaction` topic。

**数据生成规则**：
- **普通交易**：amount 10~50000，payer 1~50 随机，beneficiary 1~100 随机
- **大额交易**（触发规则）：amount 50000~500000，payer 固定 1~5（提高同 payer 命中率）
- **小额+大额配对**：同一 payer 先发 <100 小额，30秒后发 >200000 大额

### 5.2 API 端点

| 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|
| `POST` | `/mock/transaction` | 无 | 发送 1 条随机交易 |
| `POST` | `/mock/transaction/batch` | `threads`(1), `count`(100), `intervalMs`(100), `largePercent`(20) | 批量发送 |
| `POST` | `/mock/transaction/small-then-large` | `payerId`(1) | 小额+大额配对 |

---

## 六、规则配置

### 6.1 规则 JSON 格式

规则使用 `GraphSpec` JSON 描述 Pattern DAG：

```json
{
  "name": "规则名称",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "step_a",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "action == 'LOGIN_FAIL'"}
    }
  ],
  "edges": [{"source": "step_a", "target": "step_b", "type": "SKIP_TILL_NEXT"}],
  "window": {"type": "FIRST_AND_LAST", "size": 5, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "NO_SKIP", "patternName": null}
}
```

### 6.2 字段参考

#### Node（节点）

| 字段 | 说明 |
|---|---|
| `name` | 节点名称，在 edges 和输出中引用 |
| `type` | `ATOMIC` / `COMPOSITE` |
| `quantifier.consumingStrategy` | 内部事件选择策略，见下方 Consuming Strategy 详解 |
| `quantifier.properties` | 匹配次数属性，见下方 Quantifier Properties 详解 |
| `quantifier.times` | `{ "from": 3, "to": 3, "windowTime": {...} }`，循环内时间窗口见下方详解 |
| `quantifier.untilCondition` | 循环终止条件，见下方 `untilCondition` 详解 |
| `condition` | 匹配条件，见下方 Condition 详解 |

#### Consuming Strategy（`quantifier.consumingStrategy`）

控制**在同一节点内部，多次匹配到的事件之间的连续关系**。与 Edge 的 `type`（控制不同节点之间的事件关系）是两层独立的配置。

| 策略 | 对应 Flink API | 含义 |
|---|---|---|
| `STRICT` | `consecutive()` | **严格连续** — 匹配的事件之间不能有其他不匹配的事件 |
| `SKIP_TILL_NEXT` | 默认行为 | **宽松连续** — 跳过不匹配的事件，继续向后找 |
| `SKIP_TILL_ANY` | `allowCombinations()` | **非确定宽松** — 允许组合匹配，从同一事件可得多种匹配组合 |

**示例对比 — 事件流 `[A1, A2, B, A3]`，节点匹配条件 `name.startsWith('A')`，`times(3)`：**

```
事件的 name 值:  [ A1,  A2,   B,  A3 ]
                          ↑ 不匹配

STRICT (consecutive):
  要求 3 个 A 必须紧密相邻，中间不能有 B
  → 匹配失败 ❌（A2 和 A3 之间有 B 打断）

SKIP_TILL_NEXT (默认):
  跳过 B，A1→A2→A3 都满足条件
  → 匹配成功 ✅ → [A1, A2, A3]

SKIP_TILL_ANY (allowCombinations):
  同上可匹配，且还支持从同一事件出发的不同组合路径
  → 匹配成功 ✅ → 可能产生多种组合结果
```

**使用建议**：绝大多数场景使用默认 `SKIP_TILL_NEXT` 即可。`STRICT` 适用于需要事件紧密相邻的场景（如暴力破解检测中的连续登录失败）。`SKIP_TILL_ANY` 会产生组合爆炸，仅在确实需要时才使用。

#### Quantifier Properties

`properties` 定义节点的**匹配次数和匹配策略**，对应 Flink `Quantifier.QuantifierProperty`。`SINGLE`、`LOOPING`、`TIMES` 三者互斥，`OPTIONAL`、`GREEDY` 为附加修饰：

| 属性 | 对应 Flink API | 含义 |
|---|---|---|
| `SINGLE` | `where(...)` | 匹配 **恰好 1 次**（默认值，不配 times） |
| `LOOPING` | `timesOrMore(from)` | **循环匹配**，配合 `times.from` 指定最少次数，上不封顶 |
| `TIMES` | `times(from, to)` | **固定次数范围**，配合 `times.from`/`times.to` 指定上下限 |
| `OPTIONAL` | `optional()` | 节点**可选**，即使不出现也算匹配成功（可与以上任意组合） |
| `GREEDY` | `greedy()` | **贪婪匹配**，尽可能多地匹配事件（可与 LOOPING/TIMES 组合） |

**示例（以 Transaction 事件为例）：**

```json
// ① SINGLE — 恰好 1 笔大额
{"properties": ["SINGLE"], "times": null}
// → 匹配恰好 1 个 paymentAmount > 200000 的事件

// ② LOOPING — 至少 3 笔大额（连续）
{"properties": ["LOOPING"], "times": {"from": 3, "to": 3, "windowTime": null}}
// → 匹配连续 3 个 paymentAmount > 50000 的事件
// 等价 API: pattern.times(3).consecutive()

// ③ TIMES — 2 到 5 笔
{"properties": ["TIMES"], "times": {"from": 2, "to": 5, "windowTime": null}}
// → 匹配 2~5 个事件，等价 API: pattern.times(2, 5)

// ④ LOOPING + GREEDY — 贪婪循环
{"properties": ["LOOPING", "GREEDY"], "times": {"from": 1, "to": 10, "windowTime": null}}
// → 在 1~10 次范围内尽可能多地匹配

// ⑤ SINGLE + OPTIONAL — 可选节点
{"properties": ["SINGLE", "OPTIONAL"], "times": null}
// → 该节点可以出现也可以不出现，等价 API: pattern.optional()
```

#### `times.windowTime` — 循环内时间窗口

当节点使用 `LOOPING` 或 `TIMES` 时，`times.windowTime` 限制**循环内部的匹配必须在多长时间内完成**（与顶层的全局 `window` 不同）：

```json
{
  "properties": ["LOOPING"],
  "times": {
    "from": 3,
    "to": 3,
    "windowTime": {"unit": "MINUTES", "size": 1}
  }
}
```
→ 3 次匹配必须在 1 分钟内完成（等价 API: `pattern.timesOrMore(3).within(Time.minutes(1))`）

| 字段 | 作用范围 | 说明 |
|---|---|---|
| 顶层 `window` | 全局，所有节点 | 从第一个节点到最后一个节点的总时长 |
| `times.windowTime` | 单节点内 | 仅限该节点的循环匹配时长 |

#### `untilCondition` — 循环终止条件

配合 `LOOPING` 使用，指定一个**终止条件**，当遇到满足该条件的事件时停止循环：

```json
{
  "quantifier": {
    "properties": ["LOOPING"],
    "times": {"from": 1, "to": 10, "windowTime": null},
    "untilCondition": {
      "type": "AVIATOR",
      "expression": "paymentAmount > 50000"
    }
  }
}
```
→ 持续匹配，直到遇到 `paymentAmount > 50000` 的事件时停止（等价 API: `pattern.timesOrMore(1).until(...)`）

#### Edge（边）

| 策略 | 对应 API | 说明 |
|---|---|---|
| `STRICT` | `next("b")` | 严格连续 |
| `SKIP_TILL_NEXT` | `followedBy("b")` | 宽松连续（默认） |
| `SKIP_TILL_ANY` | `followedByAny("b")` | 非确定宽松 |

#### Condition（条件）

| 类型 | 格式 | 说明 |
|---|---|---|
| `AVIATOR` | `{"type":"AVIATOR","expression":"paymentAmount > 50000"}` | Aviator 表达式（推荐） |
| `CLASS` | `{"type":"CLASS","className":"com.xxx.MyCondition"}` | Java 类（支持迭代上下文） |
| `OR` | `{"type":"OR","conditions":[...]}` | 至少一个为 true |
| `AND` | `{"type":"AND","conditions":[...]}` | 全部为 true |
| `NOT` | `{"type":"NOT","condition":{...}}` | 取反 |

⚠️ **AVIATOR 限制**：只能访问当前事件字段，不能使用 `previous` 关键字。跨事件比较请使用 CLASS 类型。

**Aviator 表达式语法参考**：

| 表达式 | 说明 | 示例 |
|---|---|---|
| `==` / `!=` | 等于 / 不等于 | `action == 'LOGIN_FAIL'`, `paymentType != 'CSH'` |
| `>` / `<` / `>=` / `<=` | 比较 | `paymentAmount > 50000` |
| `&&` | 逻辑与 | `action == 'PAYMENT_FAIL' && reason == 'TIMEOUT'` |
| `\|\|` | 逻辑或 | `product == 'A' \|\| product == 'B'` |
| 字符串 | 单引号 | `action == 'VIEW_PRODUCT'` |
| 数值 | 直接写数字 | `amount > 1000`, `paymentAmount < 100` |

**规则 JSON 生成工具**：可以用 Java API 先构建 Pattern，再通过 `CepJsonUtils` 导出 JSON（避免手写 JSON 出错）：

```java
Pattern<Event, Event> pattern = Pattern.<Event>begin("start")
    .where(new AviatorCondition<>("action == 'LOGIN_FAIL'"))
    .times(3).consecutive()
    .followedBy("end")
    .where(new AviatorCondition<>("action == 'LOGIN_SUCCESS'"))
    .within(Time.minutes(5));

String json = CepJsonUtils.convertPatternToJSONString(pattern);
// → 复制到 MySQL 或直接传给 DefaultPatternProcessor
```

#### Window

```json
{"type": "FIRST_AND_LAST", "size": 5, "unit": "MINUTES"}
```

`unit`: `DAYS` / `HOURS` / `MINUTES` / `SECONDS` / `MILLISECONDS`

**`type` 时间窗口计算方式**（对应 Flink `WithinType`）：

| 类型 | 含义 | 示意图 |
|---|---|---|
| `FIRST_AND_LAST` | 从**第一个**匹配事件的时刻到**最后一个**匹配事件的时刻，总时长不能超过 `window` | `[A]──...──[B]──...──[C]` ← 这整段时间 |
| `PREVIOUS_AND_CURRENT` | 每两个连续匹配事件之间的间隔不能超过 `window` | `[A]─→[B]─→[C]` ← 每步单独计算 |

**示例对比 — 事件时间线 A(0s) → B(3min) → C(8min)，window = 5min：**

```
FIRST_AND_LAST:  A 到 C 总时长为 8min > 5min → 匹配失败 ❌
PREVIOUS_AND_CURRENT: A→B = 3min ≤ 5min ✅, B→C = 5min ≤ 5min ✅ → 匹配成功 ✅
```

**使用建议**：
- `FIRST_AND_LAST`：适合"完整序列必须在一段时间内完成"的场景，如"登录失败 3 次后在 5 分钟内登录成功"（从第一次失败到最后一次成功的全局窗口）
- `PREVIOUS_AND_CURRENT`：适合"每步之间的间隔不能太长"的场景，如"3 次连续失败每两次间隔不超过 1 分钟"，用于监控频率而不是总时长

#### AfterMatchSkipStrategy

| 策略 | 说明 |
|---|---|
| `NO_SKIP` | 返回所有可能匹配 |
| `SKIP_PAST_LAST_EVENT` | 从最后一个事件之后继续 |
| `SKIP_TO_NEXT` | 从第一个事件之后继续 |
| `SKIP_TO_FIRST` | 跳到命名模式第一个事件 |
| `SKIP_TO_LAST` | 跳到命名模式最后一个事件 |

### 6.3 内置规则

共 7 条规则，分两类：

**用户行为规则**（`db_cep_rule.sql` 规则 1-4）：

| rule_id | 场景 | 模式 |
|---|---|---|
| `rule_login_bruteforce` | 3次登录失败后5分钟内成功 | 循环3次 → 单次 |
| `rule_view_to_order` | 浏览商品后30分钟内下单 | 单次 → 单次 |
| `rule_account_hijack` | 登录→改密→大额转账(>50000) | 3步序列 |
| `rule_payment_retry` | 支付失败(OR)后2分钟内成功 | OR 条件 → 单次 |

**金融欺诈规则**（`db_cep_rule.sql` 规则 5-7）：

| rule_id | 场景 | Aviator 表达式 |
|---|---|---|
| `rule_rapid_large_amount` | 30分钟内3笔 >50000 | `paymentAmount > 50000` ×3 |
| `rule_small_then_large` | 小额(<100)→5分钟内超大额(>200000) | `paymentAmount < 100` → `paymentAmount > 200000` |
| `rule_split_transfer` | 10分钟内2笔 >30000 | `paymentAmount > 30000` ×2 |

---

## 七、规则管理 API

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/cep/rules` | 查询所有 ACTIVE 规则（Flink job 使用） |
| `GET` | `/cep/rules/all` | 查询所有规则（管理后台） |
| `GET` | `/cep/rules/hash` | 规则 hash（变更检测） |
| `GET` | `/cep/rule/{ruleId}` | 查询单个规则 |
| `POST` | `/cep/rule` | 创建/更新规则（版本号自动递增） |
| `PUT` | `/cep/rule/{ruleId}/status` | 更新状态 (1=ACTIVE, 0=PAUSED, -1=DELETED) |
| `DELETE` | `/cep/rule/{ruleId}` | 软删除 |

```bash
# 创建规则
curl -X POST http://localhost:8080/eagle-api/cep/rule \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "my_rule",
    "name": "我的规则",
    "description": "规则描述",
    "pattern": "{...GraphSpec JSON...}",
    "status": 1
  }'

# 暂停规则
curl -X PUT "http://localhost:8080/eagle-api/cep/rule/my_rule/status?status=0"

# 删除规则
curl -X PUT "http://localhost:8080/eagle-api/cep/rule/my_rule/status?status=-1"
```

---

## 八、集成方式

### 8.1 生产模式（DetectionApp 方式）

```java
// DetectionApp 完整流程:
// 1. Kafka 消费
KafkaSource<String> source = KafkaSource.<String>builder()
    .setBootstrapServers(bootstrapServers)
    .setTopics("eagle-transaction")
    .setGroupId("eagle-detection")
    .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
    .setValueOnlyDeserializer(new SimpleStringSchema())
    .build();

DataStream<Transaction> stream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka")
    .map(Transaction::fromString)
    .assignTimestampsAndWatermarks(WatermarkStrategy
        .<Transaction>forBoundedOutOfOrderness(Duration.ofMillis(60000))
        .withTimestampAssigner((txn, ts) -> txn.getEventTime()));

// 2. 启动时同步加载规则
List<PatternProcessor<Transaction>> initialRules = fetchRulesFromApi(apiBaseUrl);

// 3. Discoverer 后台轮询更新
PatternProcessorDiscovererFactory<Transaction> factory =
    new ApiPatternProcessorDiscovererFactory<>(apiBaseUrl, initialRules, 30000L);

// 4. Dynamic CEP
SingleOutputStreamOperator<String> results = DynamicCEP.dynamicPatterns(
    stream, factory, false, TypeInformation.of(String.class));
```

### 8.2 独立测试模式（CepTestDemo）

```java
// 不需要 Kafka、MySQL，规则内嵌
List<PatternProcessor<UserBehaviorEvent>> rules = CepTestDemo.buildRulesStatic();
List<UserBehaviorEvent> events = CepTestDemo.generateTestEventsStatic();

DataStream<UserBehaviorEvent> stream = env.fromCollection(events)
    .assignTimestampsAndWatermarks(...);

PatternProcessorDiscovererFactory<UserBehaviorEvent> factory =
    new ApiPatternProcessorDiscovererFactory<>("http://localhost:8080/eagle-api", rules, 30000L);

SingleOutputStreamOperator<String> results = DynamicCEP.dynamicPatterns(
    stream, factory, false, TypeInformation.of(String.class));
```

### 8.3 PatternProcessFunction 示例

```java
public class DetectionAlertFunction extends PatternProcessFunction<Transaction, String> {
    private final String ruleId;
    private final int version;

    // 必须提供 (String, int) 构造函数（JDBC Discoverer 反射使用）
    public DetectionAlertFunction(String ruleId, int version) {
        this.ruleId = ruleId;
        this.version = version;
    }

    @Override
    public void processMatch(Map<String, List<Transaction>> match,
                             Context ctx, Collector<String> out) {
        // match key = 节点 name, value = 匹配到的事件列表
        List<Transaction> largeTxns = match.get("large_txn");
        String alert = String.format("[ALERT] %s v%d: payer=%d, txnCount=%d",
            ruleId, version, largeTxns.get(0).getPayerId(), largeTxns.size());
        out.collect(alert);
    }
}
```

---

## 九、常见问题

### Q1: 为什么事件到达了但没有匹配输出？

排查步骤：
1. 规则是否已加载？观察日志 `[API-DISCOVER] ✅ Rule change detected (N rules)` 或 `[CEP] ★ Rules loaded, currently active: N`
2. EventTime 模式下，Watermark 是否推进？`env.fromCollection()` 的有界流 Watermark 会快速结束，尝试使用 Kafka 或 `DelayedEventSource`
3. 事件时间戳是否在窗口范围内？确认 `window.size` 足够大
4. Aviator 表达式是否正确？检查字段名是否匹配事件类的 getter 名（区分大小写）

### Q2: 规则更新后为什么旧的 partial match 丢失了？

当前采用**硬切换**策略：规则更新时旧版本 NFA 状态被清除，进行中的部分匹配会被丢弃。这是 MVP 的权衡（简单可靠），后续版本将通过独立 state name 实现优雅迁移。

### Q3: `SharedBuffer` 共享是否有问题？

所有规则共享一个 `SharedBuffer`，但每个规则使用独立的 state name（`{prefix}-{ruleId}-{version}`），实际无冲突。

### Q4: JDK 17 运行报 `InaccessibleObjectException`？

Flink 的 `ClosureCleaner` 需要通过反射访问 `java.base` 内部。在 IDEA 的 Run Configuration VM options 中添加：

```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.io=ALL-UNNAMED
```

（JDK 8 项目运行不需要此配置）

### Q5: 如何不依赖 MySQL / API 运行测试？

使用 `initialProcessors` 模式，规则直接内嵌在代码中（参考 `CepTestDemo`）：

```java
List<PatternProcessor<Event>> rules = Arrays.asList(
    new DefaultPatternProcessor<>("rule_1", 1, ruleJson, new MyOutputFunction("rule_1", 1))
);
new ApiPatternProcessorDiscovererFactory<>("http://localhost:8080/eagle-api", rules, 30000L);
```

Discoverer 首次检查时直接返回 `initialProcessors`，跳过 HTTP 调用。参考 `CepTestDemo.java`。

### Q6: 规则更新后多久生效？

取决于 `cep.rule.poll.interval.ms`（默认 30s）。流程：Discoverer 检测变更 → Coordinator 计算 delta → OperatorEvent 推送 → Operator 热替换。通常在 1-2 个轮询周期内生效。

---

## 十、注意事项

1. **Aviator 条件限制**：只能访问当前事件字段，无法使用 `previous`。跨事件比较请用 CLASS 类型实现 `IterativeCondition`。

2. **规则版本管理**：同 `ruleId` 提交新规则时，`version` 自动递增。Discoverer 检测版本变化后只推送变更部分。

3. **状态清理**：规则下线时旧版本的状态通过 deferred cleanup 机制清理，但不是立即的（依赖 State TTL）。

4. **SharedBuffer 共享**：所有规则共享一个 `SharedBuffer`，因使用不同 state name 无实际冲突。

5. **Flink 版本**：需要 Flink 1.17.x。

6. **依赖**：`aviator:5.3.1`、`jackson-databind`、`flink-cep`、`flink-connector-kafka`。

7. **Kafka 消息格式**：Transaction CSV 格式：`transactionId,eventTime,payerId,beneficiaryId,paymentType,paymentAmount,ingestionTimestamp`

8. **Mock 服务仅 dev 环境**：`@Profile("dev")` 标注，生产环境自动禁用。

9. **JDK 17 运行**：如使用 JDK 17+，需添加 `--add-opens` VM 参数（见 Q4）。

10. **规则 JSON 生成**：可用 `CepJsonUtils.convertPatternToJSONString(pattern)` 将 Java Pattern API 构建的规则导出为 JSON，避免手写 JSON 出错。

11. **扩展 Discoverer**：实现 `PeriodicPatternProcessorDiscoverer<T>` 抽象类，覆盖 `arePatternProcessorsUpdated()` 和 `getLatestPatternProcessors()` 即可对接 etcd/Consul/配置中心等数据源。
