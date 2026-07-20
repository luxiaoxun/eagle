-- ============================================================
-- Eagle Dynamic CEP 规则表
-- 用于存储 Flink Dynamic CEP 的模式规则
-- ============================================================

CREATE TABLE IF NOT EXISTS `eagle_cep_rule` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `rule_id`     VARCHAR(64)  NOT NULL COMMENT '规则唯一标识',
    `version`     INT          NOT NULL DEFAULT 1 COMMENT '规则版本号（递增）',
    `name`        VARCHAR(128) NOT NULL DEFAULT '' COMMENT '规则名称',
    `description` VARCHAR(512) DEFAULT '' COMMENT '规则描述',
    `pattern`     TEXT         NOT NULL COMMENT 'Pattern JSON (GraphSpec 格式)',
    `function`    VARCHAR(256) DEFAULT NULL COMMENT 'PatternProcessFunction 全限定类名（可选）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=ACTIVE, 0=PAUSED, -1=DELETED',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_id_version` (`rule_id`, `version`),
    INDEX `idx_rule_id_status` (`rule_id`, `status`),
    INDEX `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Eagle Dynamic CEP 规则表';

-- ============================================================
-- 示例规则数据：用户行为异常检测
-- ============================================================

-- 规则 1：用户连续 3 次登录失败后，在 5 分钟内登录成功
-- 场景：暴力破解检测
INSERT INTO `eagle_cep_rule` (`rule_id`, `version`, `name`, `description`, `pattern`, `function`, `status`) VALUES
('rule_login_bruteforce', 1, '登录暴力破解检测', '连续3次登录失败后5分钟内登录成功',
'{
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
      "condition": {"type": "AVIATOR", "expression": "action == ''LOGIN_FAIL''"}
    },
    {
      "name": "login_success",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "action == ''LOGIN_SUCCESS''"}
    }
  ],
  "edges": [
    {"source": "login_fail", "target": "login_success", "type": "SKIP_TILL_NEXT"}
  ],
  "window": {"type": "FIRST_AND_LAST", "size": 5, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "NO_SKIP", "patternName": null}
}', NULL, 1);

-- 规则 2：用户浏览商品后 30 分钟内下单
-- 场景：正常转化漏斗监控
-- 注意：Aviator 表达式不能使用 previous 关键字（SimpleCondition 无迭代上下文），
-- 如需跨事件条件比较，使用 CLASS 类型的 IterativeCondition 实现
INSERT INTO `eagle_cep_rule` (`rule_id`, `version`, `name`, `description`, `pattern`, `function`, `status`) VALUES
('rule_view_to_order', 1, '浏览到下单转化', '用户浏览商品后30分钟内下单',
'{
  "name": "view_to_order",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "view_product",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "action == ''VIEW_PRODUCT''"}
    },
    {
      "name": "place_order",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "action == ''PLACE_ORDER''"}
    }
  ],
  "edges": [
    {"source": "view_product", "target": "place_order", "type": "SKIP_TILL_NEXT"}
  ],
  "window": {"type": "FIRST_AND_LAST", "size": 30, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "SKIP_PAST_LAST_EVENT", "patternName": null}
}', NULL, 1);

-- 规则 3：高风险操作序列：登录 → 修改密码 → 大额转账
-- 场景：账户盗用检测
INSERT INTO `eagle_cep_rule` (`rule_id`, `version`, `name`, `description`, `pattern`, `function`, `status`) VALUES
('rule_account_hijack', 1, '账户盗用检测', '登录后10分钟内修改密码，随后30分钟内大额转账(>50000)',
'{
  "name": "account_hijack",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "login",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "action == ''LOGIN_SUCCESS''"}
    },
    {
      "name": "change_pwd",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "action == ''CHANGE_PASSWORD''"}
    },
    {
      "name": "large_transfer",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "action == ''TRANSFER'' && amount > 50000"}
    }
  ],
  "edges": [
    {"source": "login", "target": "change_pwd", "type": "SKIP_TILL_NEXT"},
    {"source": "change_pwd", "target": "large_transfer", "type": "SKIP_TILL_NEXT"}
  ],
  "window": {"type": "FIRST_AND_LAST", "size": 40, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "NO_SKIP", "patternName": null}
}', NULL, 1);

-- 规则 4：支付失败后立即重试成功（使用 OR 条件）
-- 场景：支付异常恢复监控
INSERT INTO `eagle_cep_rule` (`rule_id`, `version`, `name`, `description`, `pattern`, `function`, `status`) VALUES
('rule_payment_retry', 1, '支付重试成功检测', '支付失败(余额不足 OR 超时)后 2 分钟内重试成功',
'{
  "name": "payment_retry",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "payment_fail",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {
        "type": "OR",
        "conditions": [
          {"type": "AVIATOR", "expression": "action == ''PAYMENT_FAIL'' && reason == ''BALANCE_NOT_ENOUGH''"},
          {"type": "AVIATOR", "expression": "action == ''PAYMENT_FAIL'' && reason == ''TIMEOUT''"}
        ]
      }
    },
    {
      "name": "payment_success",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "action == ''PAYMENT_SUCCESS''"}
    }
  ],
  "edges": [
    {"source": "payment_fail", "target": "payment_success", "type": "SKIP_TILL_NEXT"}
  ],
  "window": {"type": "FIRST_AND_LAST", "size": 2, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "NO_SKIP", "patternName": null}
}', NULL, 1);

-- ============================================================
-- 金融欺诈检测规则
-- ============================================================

-- 规则 5：快速多笔大额交易
-- 场景：同一 payer 30 分钟内 3 笔以上超过 50000 的交易（替代原聚合规则）
INSERT INTO `eagle_cep_rule` (`rule_id`, `version`, `name`, `description`, `pattern`, `function`, `status`) VALUES
('rule_rapid_large_amount', 1, '快速多笔大额交易', '同一payer 30分钟内3笔以上超过50000的交易',
'{
  "name": "rapid_large_amount",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "large_txn",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "STRICT",
        "properties": ["LOOPING"],
        "times": {"from": 3, "to": 3, "windowTime": null}
      },
      "condition": {"type": "AVIATOR", "expression": "paymentAmount > 50000"}
    }
  ],
  "edges": [],
  "window": {"type": "FIRST_AND_LAST", "size": 30, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "SKIP_PAST_LAST_EVENT", "patternName": null}
}', NULL, 1);

-- 规则 6：小额测试后超大额转账
-- 场景：同一 payer 先做 <100 小额测试，5 分钟内做 >200000 超大额转账
INSERT INTO `eagle_cep_rule` (`rule_id`, `version`, `name`, `description`, `pattern`, `function`, `status`) VALUES
('rule_small_then_large', 1, '小额测试后大额转账', '同一payer先做<100小额交易测试，5分钟内做>200000超大额交易',
'{
  "name": "small_then_large",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "small_txn",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "paymentAmount < 100"}
    },
    {
      "name": "huge_txn",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "paymentAmount > 200000"}
    }
  ],
  "edges": [
    {"source": "small_txn", "target": "huge_txn", "type": "SKIP_TILL_NEXT"}
  ],
  "window": {"type": "FIRST_AND_LAST", "size": 5, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "NO_SKIP", "patternName": null}
}', NULL, 1);

-- 规则 7：分散转出检测
-- 场景：同一 payer 10 分钟内做 2 笔以上大额转账到不同 beneficiary
INSERT INTO `eagle_cep_rule` (`rule_id`, `version`, `name`, `description`, `pattern`, `function`, `status`) VALUES
('rule_split_transfer', 1, '分散转出检测', '同一payer 10分钟内做2笔以上>30000转账到不同beneficiary',
'{
  "name": "split_transfer",
  "type": "COMPOSITE",
  "version": 1,
  "nodes": [
    {
      "name": "first_txn",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "paymentAmount > 30000"}
    },
    {
      "name": "second_txn",
      "type": "ATOMIC",
      "quantifier": {
        "consumingStrategy": "SKIP_TILL_NEXT",
        "properties": ["SINGLE"],
        "times": null
      },
      "condition": {"type": "AVIATOR", "expression": "paymentAmount > 30000"}
    }
  ],
  "edges": [
    {"source": "first_txn", "target": "second_txn", "type": "SKIP_TILL_NEXT"}
  ],
  "window": {"type": "FIRST_AND_LAST", "size": 10, "unit": "MINUTES"},
  "afterMatchStrategy": {"type": "NO_SKIP", "patternName": null}
}', NULL, 1);
