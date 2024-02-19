package com.alarm.eagle.rule;

import com.alarm.eagle.response.Response;
import com.alarm.eagle.util.HttpUtil;
import com.alarm.eagle.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RuleUtil {
    private static final Logger logger = LoggerFactory.getLogger(RuleUtil.class);

    public static RuleBase getRules(String ruleUrl) {
        RuleBase ruleBase = null;
        try {
            String content = HttpUtil.doGet(ruleUrl);
//            String content = HttpUtil.doGetMock(ruleUrl);
            if (content == null) {
                logger.error("Failed to get rules from url {}", ruleUrl);
                return ruleBase;
            }

            Response<List<Rule>> response = JsonUtil.decode(content, new TypeReference<Response<List<Rule>>>() {
            });
            if (response == null || response.getData() == null || !response.getCode().equals("200")) {
                logger.error("Failed to get rules content: {}", content);
                return ruleBase;
            }

            List<Rule> ruleList = response.getData();
            logger.info("Get rule list: {}", JsonUtil.encode(ruleList));
            ruleBase = new RuleBase(ruleList);

        } catch (Exception e) {
            logger.error("Exception occurs:", e);
        }
        return ruleBase;
    }

    public static RuleBase getMockRules(String ruleUrl) {
        List<Rule> ruleList = new ArrayList<>();

        Rule rule1 = new Rule();
        rule1.setId("1");
        rule1.setName("log_app_1");
        rule1.setType("log-rules");
        rule1.setScript("package logrules\n" +
                "import com.alarm.eagle.util.DateUtil;\n" +
                "import com.alarm.eagle.log.LogEvent;\n" +
                "import org.slf4j.Logger;\n" +
                "import com.alarm.eagle.util.Md5Util;\n" +
                "import com.alarm.eagle.util.RegexUtil\n" +
                "import java.util.Date;\n" +
                "global Logger LOG;\n" +
                "rule \"log_app_1\"\n" +
                "    no-loop true\n" +
                "    salience 100\n" +
                "    when\n" +
                "        $log : LogEvent( index == \"log_app_1\", $msg : message)\n" +
                "    then\n" +
                "        LOG.debug(\"receive log_app_1 log, id:[{}]\", $log.getId());\n" +
                "        String logTime = RegexUtil.extractString(\"(\\\\d{4}-\\\\d{2}-\\\\d{2} \\\\d{2}:\\\\d{2}:\\\\d{2})\", $msg);\n" +
                "        if (logTime == null) {\n" +
                "            LOG.warn(\"invalid date or time, log: {}\", $msg);\n" +
                "            return;\n" +
                "        }\n" +
                "        Date date = DateUtil.convertFromString(\"yyyy-MM-dd HH:mm:ss\", logTime);\n" +
                "        $log.setTimestamp(date != null ? date : $log.getAtTimestamp());\n" +
                "\n" +
                "        long delayTime = (System.currentTimeMillis() - $log.getTimestamp().getTime())/1000;\n" +
                "        if (delayTime > 5*24*3600 || delayTime < -5*24*3600) {\n" +
                "            LOG.warn(\"Too early or too late log, ignore it, delay:{}, log:{}\", delayTime, $log.getTimestamp().getTime());\n" +
                "            return;\n" +
                "        }\n" +
                "        $log.dealDone();\n" +
                "        LOG.debug(\"out -----log_app_1------\");\n" +
                "end\n");
        rule1.setState("1");
        ruleList.add(rule1);

        Rule rule2 = new Rule();
        rule2.setId("2");
        rule2.setName("log_app_2");
        rule2.setType("log-rules");
        rule2.setScript("package logrules\n" +
                "import com.alarm.eagle.util.DateUtil;\n" +
                "import com.alarm.eagle.log.LogEvent;\n" +
                "import org.slf4j.Logger;\n" +
                "import com.alarm.eagle.util.Md5Util;\n" +
                "import com.alarm.eagle.util.RegexUtil\n" +
                "import java.util.Date;\n" +
                "global Logger LOG;\n" +
                "rule \"log_app_2\"\n" +
                "    no-loop true\n" +
                "    salience 100\n" +
                "    when\n" +
                "        $log : LogEvent( index == \"log_app_2\", $msg : message)\n" +
                "    then\n" +
                "        LOG.debug(\"receive log_app_2 log, id:[{}]\", $log.getId());\n" +
                "        String logTime = RegexUtil.extractString(\"(\\\\d{4}-\\\\d{2}-\\\\d{2} \\\\d{2}:\\\\d{2}:\\\\d{2})\", $msg);\n" +
                "        if (logTime == null) {\n" +
                "            LOG.warn(\"invalid date or time, log: {}\", $msg);\n" +
                "            return;\n" +
                "        }\n" +
                "        Date date = DateUtil.convertFromString(\"yyyy-MM-dd HH:mm:ss\", logTime);\n" +
                "        $log.setTimestamp(date != null ? date : $log.getAtTimestamp());\n" +
                "\n" +
                "        $log.dealDone();\n" +
                "        LOG.debug(\"out -----log_app_2------\");\n" +
                "end\n");
        rule2.setState("1");
        ruleList.add(rule2);

        return new RuleBase(ruleList);
    }

}
