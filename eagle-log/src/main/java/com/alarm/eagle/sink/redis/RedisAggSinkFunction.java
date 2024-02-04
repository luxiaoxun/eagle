package com.alarm.eagle.sink.redis;

import com.alarm.eagle.config.ConfigConstant;
import com.alarm.eagle.util.DateUtil;
import com.alarm.eagle.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;

import java.util.*;

/**
 * Created by luxiaoxun on 2020/06/15.
 */
public class RedisAggSinkFunction extends RichSinkFunction<LogStatWindowResult> {
    private static final Logger logger = LoggerFactory.getLogger(RedisSinkFunction.class);

    private transient JedisCluster jedisCluster = null;

    private transient JedisPool jedisPool = null;

    private boolean isRedisCluster = false;

    @Override
    public void open(Configuration parameters) throws Exception {
        ParameterTool parameterTool = (ParameterTool)
                getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
        isRedisCluster = parameterTool.getBoolean(ConfigConstant.REDIS_CLUSTER_ENABLED, false);
        if (isRedisCluster) {
            jedisCluster = getJedisCluster(parameterTool);
        } else {
            jedisPool = getJedisPool(parameterTool);
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (jedisCluster != null) {
            jedisCluster.close();
        }
        if (jedisPool != null) {
            jedisPool.close();
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
            if (isRedisCluster) {
                for (String keyIp : statMap.keySet()) {
                    Long count = statMap.get(keyIp);
                    jedisCluster.hincrBy(redisKey, keyIp, count);
                    logger.debug("item.key:{},item.value:{}", keyIp, count);
                    totalCount += count;
                }
                jedisCluster.expire(redisKey, 2 * 24 * 3600);  // 48 hours
                String redisTotalKey = preKey + keyName + ":total:" + postKey;
                logger.info("Redis total.key:{}, total.value:{}", redisTotalKey, totalCount);
                jedisCluster.set(redisTotalKey, Long.toString(totalCount));
                jedisCluster.expire(redisTotalKey, 2 * 24 * 3600);  // 48 hours
            } else {
                try (Jedis jedis = jedisPool.getResource()) {
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
                }
            }
        } catch (Exception ex) {
            logger.error("Redis sink, key name:{}, exception:{}", keyName, ex.toString());
        }

    }

    private JedisCluster getJedisCluster(ParameterTool parameter) {
        JedisCluster jedis = null;
        String redisHosts = parameter.get(ConfigConstant.REDIS_HOSTS, "");
        String redisPassword = parameter.get(ConfigConstant.REDIS_PASSWORD, "");
        if (!StringUtils.isEmpty(redisHosts)) {
            Set<HostAndPort> nodes = new HashSet<>();
            for (String server : redisHosts.split(",")) {
                String[] hostPort = server.split(":");
                nodes.add(new HostAndPort(hostPort[0], hostPort.length >= 2 ? Integer.parseInt(hostPort[1]) : 6379));
            }
            JedisPoolConfig jpc = new JedisPoolConfig();
            jpc.setMaxTotal(8);
            jpc.setMaxIdle(8);
            jpc.setMaxWaitMillis(1000L);
            int timeout = 3000;
            int maxAttempts = 10;
            if (!StringUtil.isEmpty(redisPassword)) {
                jedis = new JedisCluster(nodes, timeout, timeout, maxAttempts, redisPassword, jpc);
            } else {
                jedis = new JedisCluster(nodes, timeout, maxAttempts, jpc);
            }
        }
        return jedis;
    }

    private JedisPool getJedisPool(ParameterTool parameter) {
        JedisPool jedis = null;
        String redisHosts = parameter.get(ConfigConstant.REDIS_HOSTS, "");
        String redisPassword = parameter.get(ConfigConstant.REDIS_PASSWORD, "");
        if (!StringUtils.isEmpty(redisHosts)) {
            String[] hostPort = redisHosts.split(":");
            if (hostPort.length == 2) {
                JedisPoolConfig jpc = new JedisPoolConfig();
                jpc.setMaxTotal(8);
                jpc.setMaxIdle(8);
                jpc.setMaxWaitMillis(1000L);
                int timeout = 3000;
                if (!StringUtil.isEmpty(redisPassword)) {
                    jedis = new JedisPool(jpc, hostPort[0], Integer.parseInt(hostPort[1]), timeout, redisPassword);
                } else {
                    jedis = new JedisPool(jpc, hostPort[0], Integer.parseInt(hostPort[1]), timeout);
                }
            }
        }
        return jedis;
    }
}
