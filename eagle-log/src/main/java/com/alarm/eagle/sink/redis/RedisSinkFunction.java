package com.alarm.eagle.sink.redis;

import com.alarm.eagle.log.LogEvent;
import com.alarm.eagle.util.DateUtil;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by luxiaoxun on 2020/01/29.
 */
public class RedisSinkFunction extends RichSinkFunction<Tuple2<String, List<LogEvent>>> {
    private static final Logger logger = LoggerFactory.getLogger(RedisSinkFunction.class);

    private transient RedisClient redisClient = null;

    @Override
    public void open(Configuration parameters) throws Exception {
        ParameterTool parameterTool = (ParameterTool) getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
        redisClient = RedisUtil.getRedisClient(parameterTool);
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    @Override
    public void invoke(Tuple2<String, List<LogEvent>> value, Context context) {
        String keyName = value.getField(0);
        logger.debug("Redis sink, key name: " + keyName);
        List<LogEvent> logs = value.getField(1);
        if (logs != null && logs.size() > 0) {
            Map<String, Long> statMap = new HashMap<>();
            for (LogEvent entry : logs) {
                String ip = entry.getIp();
                String index = entry.getIndex();
                logger.debug("receive ip:{}, index:{}", ip, index);
                if (ip == null) {
                    logger.warn("ip is null, index:{}", index);
                    ip = "127.0.0.1";
                }
                if (statMap.containsKey(ip)) {
                    statMap.put(ip, statMap.get(ip) + 1);
                } else {
                    statMap.put(ip, 1L);
                }
            }

            try {
                Date now = DateUtil.getNowDateMinute();
                long totalCount = 0;
                String preKey = "log:";
                String postKey = DateUtil.convertToUTCString("yyyyMMdd:HHmm", now.getTime());
                String redisKey = preKey + keyName + ":" + postKey;
                logger.info("redis key:{}", redisKey);

                try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
                    RedisCommands<String, String> syncCommands = connection.sync();
                    for (String key : statMap.keySet()) {
                        Long count = statMap.get(key);
                        syncCommands.hincrby(redisKey, key, count);
                        logger.debug("item.key:{},item.value:{}", key, count);
                        totalCount += count;
                    }
                    syncCommands.expire(redisKey, 2 * 24 * 3600);  // 48 hours
                    String redisTotalKey = preKey + keyName + ":total:" + postKey;
                    logger.info("Redis total.key:{}, total.value:{}", redisTotalKey, totalCount);
                    syncCommands.set(redisTotalKey, Long.toString(totalCount));
                    syncCommands.expire(redisTotalKey, 2 * 24 * 3600);  // 48 hours
                }
            } catch (Exception ex) {
                logger.error("Redis sink, key name:{}, exception:{}", keyName, ex.toString());
            }
        }
    }
}
