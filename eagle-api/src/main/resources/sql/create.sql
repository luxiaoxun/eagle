CREATE TABLE `eagle_task` (
  `task_id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '任务id',
  `task_name` varchar(255) NOT NULL DEFAULT '' COMMENT '任务名称',
  `time_characteristic` tinyint(2) NOT NULL DEFAULT '0' COMMENT 'flink time类型 0 processingtime 1 ingestiontime 2 eventtime',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '状态,0：停用，1：启用',
  `annotation` text COMMENT '注解',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预警任务表';


CREATE TABLE `eagle_datasource` (
  `datasource_id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '数据源id',
  `task_id` int(11) NOT NULL DEFAULT '0' COMMENT '任务id',
  `topic` varchar(255) NOT NULL DEFAULT '' COMMENT '队列topic',
  `servers` varchar(1024) NOT NULL DEFAULT '' COMMENT '队列地址列表',
  `group_id` varchar(255) NOT NULL DEFAULT '' COMMENT '队列groupid',
  `stream_id` varchar(255) NOT NULL DEFAULT '' COMMENT '对应siddhi streamid',
  `event_timestamp_field` varchar(255) NOT NULL DEFAULT '' COMMENT '时间戳字段',
  `key_by_fields` varchar(255) NOT NULL DEFAULT '' COMMENT 'keyby分组字段(,分割)',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '状态,0：停用，1：启用',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `filter` text COMMENT '过滤表达式',
  PRIMARY KEY (`datasource_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预警任务数据源配置';

CREATE TABLE `eagle_app` (
  `app_id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '应用id',
  `app_name` varchar(255) NOT NULL DEFAULT '' COMMENT '应用名称',
  `task_id` int(11) NOT NULL DEFAULT '0' COMMENT '任务id',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '状态,0：停用，1：启用',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`app_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预警任务应用配置';


CREATE TABLE `eagle_app_stream_define` (
  `stream_define_id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `stream_id` varchar(255) NOT NULL DEFAULT '' COMMENT '对应siddhi streamid',
  `task_id` int(11) NOT NULL DEFAULT '0' COMMENT '任务id',
  `app_id` int(11) NOT NULL DEFAULT '0' COMMENT '应用id',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`stream_define_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预警任务流属性配置';


CREATE TABLE `eagle_app_stream_field` (
  `field_id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `stream_define_id` int(11) NOT NULL DEFAULT '0' COMMENT '对应stream_define id',
  `task_id` int(11) NOT NULL DEFAULT '0' COMMENT '任务id',
  `app_id` int(11) NOT NULL DEFAULT '0' COMMENT '应用id',
  `field_name` varchar(255) NOT NULL DEFAULT '' COMMENT '字段名称',
  `field_type` varchar(255) NOT NULL DEFAULT '' COMMENT '字段类型',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`field_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预警任务流属性字段配置';

CREATE TABLE `eagle_app_policy` (
  `policy_id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '策略id',
  `policy_name` varchar(255) NOT NULL DEFAULT '' COMMENT '策略名称',
  `metric` varchar(255) NOT NULL DEFAULT '' COMMENT '策略聚合指标,类tag功能',
  `task_id` int(11) NOT NULL DEFAULT '0' COMMENT '任务id',
  `app_id` int(11) NOT NULL DEFAULT '0' COMMENT '应用id',
  `cql` text COMMENT '策略cql',
  `alert_level` int(11) NOT NULL DEFAULT '0' COMMENT '报警级别,1恢复 2警告 3紧急 4严重',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '状态,0：停用，1：启用',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `prepare_stream` text COMMENT '准备流结构定义',
  PRIMARY KEY (`policy_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预警任务策略配置';

CREATE TABLE `eagle_sink` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `task_id` int(11) NOT NULL DEFAULT '0' COMMENT '任务id',
  `app_id` int(11) NOT NULL DEFAULT '0' COMMENT '应用id',
  `metric` varchar(255) NOT NULL DEFAULT '' COMMENT '策略聚合指标,类tag功能',
  `policy_id` int(11) NOT NULL DEFAULT '0' COMMENT '策略id',
  `data` text COMMENT '结果',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_app_policy` (`create_time`,`policy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预警sink结果';


 CREATE TABLE `eagle_alert_group` (
  `group_id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `group_name` varchar(255) NOT NULL DEFAULT '' COMMENT '组名称',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预警告警组';

CREATE TABLE `eagle_alert_group_user` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `group_id` int(11) NOT NULL DEFAULT '0' COMMENT '组id',
  `user_name` varchar(255) NOT NULL DEFAULT '' COMMENT '用户名称',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '状态,0：停用，1：启用',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预警告警组成员';

CREATE TABLE `eagle_alert_policy` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `policy_id` int(11) NOT NULL DEFAULT '0' COMMENT '策略id',
  `alert_type` int(11) NOT NULL DEFAULT '0' COMMENT '告警方式 1回调 2邮件 4微信 8短信',
  `cd` int(11) NOT NULL DEFAULT '0' COMMENT 'cd时间,单位秒',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '状态,0：停用，1：启用',
  `group_id` int(11) NOT NULL DEFAULT '0' COMMENT '报警组',
  `txt_template` text COMMENT 'txt模板',
  `html_template` text COMMENT 'html模板',
  `callback` varchar(512) NOT NULL DEFAULT '' COMMENT '回调地址',
  `last_alert_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '上一次通知时间',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_policy_id` (`policy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='policy告警配置';


CREATE TABLE `eagle_alert_user` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `user_name` varchar(255) NOT NULL DEFAULT '' COMMENT '用户名称',
  `phone` varchar(255) NOT NULL DEFAULT '' COMMENT '手机号码',
  `mail` varchar(255) NOT NULL DEFAULT '' COMMENT '邮箱',
  `weixin_name` varchar(255) NOT NULL DEFAULT '' COMMENT '微信用户名',
  `create_time` datetime NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预警用户配置';

