package com.alarm.eagle.redis;

import com.alarm.eagle.config.ConfigConstant;
import com.alarm.eagle.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;

/**
 * Created by luxiaoxun on 2020/06/15.
 */
public class RedisAggSinkFunction extends RichSinkFunction<LogStatWindowResult> {
    private static final Logger logger = LoggerFactory.getLogger(RedisSinkFunction.class);

    private transient JedisCluster jedis = null;

    @Override
    public void open(Configuration parameters) throws Exception {
        ParameterTool parameterTool = (ParameterTool)
                getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
        jedis = getJedis(parameterTool);
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (jedis != null) {
            jedis.close();
        }
    }

    @Override
    public void invoke(LogStatWindowResult value, Context context) {
        String keyName = value.getKey();
        logger.debug("Redis sink, key name: " + keyName);
        Map<String, Long> statMap = value.getIpMap();
        try {
            long totalCount = 0;
            String preKey = "log:";
            String postKey = DateUtil.convertToUTCString("yyyyMMdd:HHmm", value.getEndTime());
            String redisKey = preKey + keyName + ":" + postKey;
            logger.info("redis key:{}", redisKey);
            for (String keyIp : statMap.keySet()) {
                Long count = statMap.get(keyIp);
                jedis.hincrBy(redisKey, keyIp, count);
                logger.debug("item.key:{},item.value:{}", keyIp, count);
                totalCount += count;
            }
            jedis.expire(redisKey, 2 * 24 * 3600);  // 48 hours
            String redisTotalKey = preKey + keyName + ":total:" + postKey;
            logger.info("Redis total.key:{}, total.value:{}", redisTotalKey, totalCount);
            jedis.set(redisTotalKey, Long.toString(totalCount));
            jedis.expire(redisTotalKey, 2 * 24 * 3600);  // 48 hours
        } catch (Exception ex) {
            logger.error("Redis sink, key name:{}, exception:{}", keyName, ex.toString());
        }

    }

    private JedisCluster getJedis(ParameterTool parameter) {
        JedisCluster jedis = null;
        String redisCluster = parameter.get(ConfigConstant.REDIS_CLUSTER_HOSTS, "");
        if (!StringUtils.isEmpty(redisCluster)) {
            Set<HostAndPort> nodes = new HashSet<>();
            for (String server : redisCluster.split(",")) {
                String[] hostPort = server.split(":");
                nodes.add(new HostAndPort(hostPort[0], hostPort.length >= 2 ? Integer.parseInt(hostPort[1]) : 6379));
            }
            JedisPoolConfig jpc = new JedisPoolConfig();
            jpc.setMaxTotal(8);
            jpc.setMaxIdle(8);
            jpc.setMaxWaitMillis(1000L);
            int timeout = 3000;
            int maxAttempts = 10;
            jedis = new JedisCluster(nodes, timeout, maxAttempts, jpc);
        }
        return jedis;
    }
}
