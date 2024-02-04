package com.alarm.eagle.alert.config;

import com.alarm.eagle.alert.constant.PropertiesConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by skycrab on 18/1/4.
 */
public class EagleProperties {
    private static final Logger logger = LoggerFactory.getLogger(EagleProperties.class);

    Properties properties;

    private static class PropertiesHolder {
        private static final EagleProperties holder = new EagleProperties();
    }

    public static EagleProperties getInstance() {
        return PropertiesHolder.holder;
    }

    private EagleProperties() {
        properties = new Properties();
        init();
    }

    private void init() {
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        } catch (Exception e) {
            logger.error("perperties init error=", e);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public boolean isDevMode() {
        String mode = getProperty(PropertiesConstant.MODE_NAME);
        if (PropertiesConstant.MODE_DEV.equals(mode)) {
            return true;
        }
        return false;
    }

    /**
     * 获取Java Opt是否配置checkpoint
     *
     * @return
     */
    public boolean enableCheckPoint() {
        String flag = System.getProperty(PropertiesConstant.ENABLE_CHECKPOINT_NAME);
        if (flag != null && flag.equalsIgnoreCase("false")) {
            return false;
        }

        return true;
    }

    /**
     * 获取Java opt并行度参数
     *
     * @return
     */
    public Integer getParallelism() {
        String value = System.getProperty(PropertiesConstant.PARALLELISM_NAME);
        if (value != null) {
            return Integer.valueOf(value);
        }
        return null;
    }
}
