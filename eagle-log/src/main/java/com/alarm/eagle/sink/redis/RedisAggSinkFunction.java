package com.alarm.eagle.sink.redis;

import com.alarm.eagle.util.DateUtil;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by luxiaoxun on 2020/06/15.
 */
public class RedisAggSinkFunction extends RichSinkFunction<LogStatWindowResult> {
    private static final Logger logger = LoggerFactory.getLogger(RedisAggSinkFunction.class);

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
    public void invoke(LogStatWindowResult value, Context context) {
        String keyName = value.getKey();
        logger.info("Redis sink, key name: " + keyName);
        Map<String, Long> statMap = value.getIpMap();
        try {
            long totalCount = 0;
            String preKey = "log:";
            String postKey = DateUtil.convertToUTCString("yyyyMMdd:HHmm", value.getEndTime());
            String redisKey = preKey + keyName + ":" + postKey;
            logger.info("Redis key:{}", redisKey);
            try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
                RedisCommands<String, String> syncCommands = connection.sync();
                for (String keyIp : statMap.keySet()) {
                    Long count = statMap.get(keyIp);
                    syncCommands.hincrby(redisKey, keyIp, count);
                    logger.debug("Redis item.key:{}, item.value:{}", keyIp, count);
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
