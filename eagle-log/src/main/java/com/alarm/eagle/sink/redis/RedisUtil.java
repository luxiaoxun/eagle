package com.alarm.eagle.sink.redis;

import com.alarm.eagle.config.ConfigConstant;
import com.alarm.eagle.util.StringUtil;
import io.lettuce.core.RedisClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;

public class RedisUtil {
    public static RedisClient getRedisClient(ParameterTool parameter) {
        RedisClient client = null;
        String redisHosts = parameter.get(ConfigConstant.REDIS_HOSTS, "");
        String redisPassword = parameter.get(ConfigConstant.REDIS_PASSWORD, "");
        if (!StringUtils.isEmpty(redisHosts)) {
            String[] hostPort = redisHosts.split(":");
            if (hostPort.length == 2) {
                if (!StringUtil.isEmpty(redisPassword)) {
                    String redisUri = String.format("redis://%s@%s:%s", redisPassword, hostPort[0], hostPort[1]);
                    client = RedisClient.create(redisUri);
                } else {
                    String redisUri = String.format("redis://%s:%s", hostPort[0], hostPort[1]);
                    client = RedisClient.create(redisUri);
                }
            }
        }
        return client;
    }
}
