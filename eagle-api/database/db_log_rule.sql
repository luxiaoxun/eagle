CREATE TABLE IF NOT EXISTS `eagle_log_rule` (
  `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name`        varchar(256) NOT NULL COMMENT '规则名',
  `app_id`      varchar(128) NOT NULL COMMENT 'app id',
  `version`     varchar(128) NOT NULL COMMENT '版本',
  `type`        varchar(64) NOT NULL COMMENT '规则类型',
  `script`      text NOT NULL COMMENT '规则',
  `state`       int(11) NOT NULL COMMENT '状态 1有效 0无效',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `app_id` (`app_id`,`version`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志解析规则表';

INSERT INTO `eagle_log_rule` (`id`, `name`, `app_id`, `version`, `type`, `script`, `state`, `update_time`) VALUES (
'1', 'log_app_1', '123', '20200101', 'log-rules', 'package logrules
import com.alarm.eagle.util.DateUtil;
import com.alarm.eagle.log.LogEntry;
import org.slf4j.Logger;
import com.alarm.eagle.util.Md5Util;
import com.alarm.eagle.util.RegexUtil
import java.util.Date;
global Logger LOG;
rule "log_app_1"
    no-loop true
    salience 100
    when
        $log : LogEntry( index == "log_app_1", $msg : message)
    then
        LOG.debug("receive log_app_1 log, id:[{}]", $log.getId());
        String logTime = RegexUtil.extractString("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3})", $msg);
        if (logTime == null) {
            LOG.warn("invalid date or time, log: {}", $msg);
            return;
        }
        Date date = DateUtil.convertFromString("yyyy-MM-dd HH:mm:ss.SSS", logTime);
        $log.setTimestamp(date != null ? date : $log.getAtTimestamp());

        long delayTime = (System.currentTimeMillis() - $log.getTimestamp().getTime())/1000;
        if (delayTime > 5*24*3600 || delayTime < -5*24*3600) {
            LOG.warn("Too early or too late log, ignore it, delay:{}, log:{}", delayTime, $log.getTimestamp().getTime());
            return;
        }
        $log.dealDone();
        LOG.debug("out -----log_app_1------");
end', '1', '2020-01-01 16:33:31.452');

INSERT INTO `eagle_log_rule` (`id`, `name`, `app_id`, `version`, `type`, `script`, `state`, `update_time`) VALUES (
'2', 'log_app_2', '456', '20200102', 'log-rules', 'package logrules
import com.alarm.eagle.util.DateUtil;
import com.alarm.eagle.log.LogEntry;
import org.slf4j.Logger;
import com.alarm.eagle.util.Md5Util;
import com.alarm.eagle.util.RegexUtil
import java.util.Date;
global Logger LOG;
rule "log_app_2"
    no-loop true
    salience 100
    when
        $log : LogEntry( index == "log_app_2", $msg : message)
    then
        LOG.debug("receive log_app_2 log, id:[{}]", $log.getId());
        String logTime = RegexUtil.extractString("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})", $msg);
        if (logTime == null) {
            LOG.warn("invalid date or time, log: {}", $msg);
            return;
        }
        Date date = DateUtil.convertFromString("yyyy-MM-dd HH:mm:ss", logTime);
        $log.setTimestamp(date != null ? date : $log.getAtTimestamp());

        $log.dealDone();
        LOG.debug("out -----log_app_2------");
end', '1', '2020-01-02 19:39:17.521');
