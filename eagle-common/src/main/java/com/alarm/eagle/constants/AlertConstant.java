package com.alarm.eagle.constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 报警常量
 * Created by luxiaoxun on 17/12/26.
 */
public class AlertConstant {
    /**
     * 报警级别
     */
    public enum AlertLevel {
        /**
         * 恢复
         */
        OK(1),

        /**
         * 警告
         */
        WARN(2),

        /**
         * 紧急
         */
        CRITICAL(3),

        /**
         * 严重
         */
        FATAL(4);

        private int id;

        AlertLevel(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static AlertLevel resolve(int id) {
            for (AlertLevel alertLevel : values()) {
                if (alertLevel.getId() == id) {
                    return alertLevel;
                }
            }
            throw new IllegalArgumentException("unknown alert level,id:" + id);
        }
    }

    /**
     * 状态
     */
    public enum Status {
        /**
         * 禁用
         */
        DISABLE(0),

        /**
         * 启用
         */
        ENABLE(1);

        private int id;

        Status(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Status resolve(int id) {
            for (Status status : values()) {
                if (status.getId() == id) {
                    return status;
                }
            }
            throw new IllegalArgumentException("unknown alert status,id: " + id);
        }

    }

    /**
     * flink time类型
     */
    public enum TimeCharacteristic {
        ProcessingTime(0),
        IngestionTime(1),
        EventTime(2);

        private int id;

        TimeCharacteristic(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static TimeCharacteristic resolve(int id) {
            for (TimeCharacteristic timeCharacteristic : values()) {
                if (timeCharacteristic.getId() == id) {
                    return timeCharacteristic;
                }
            }
            throw new IllegalArgumentException("unknown timeCharacteristic, id: " + id);
        }
    }

    /**
     * 告警方式
     */
    public enum AlertType {
        /**
         * http回调
         */
        HttpCallback(1),

        /**
         * 邮件
         */
        Mail(2),

        /**
         * 微信
         */
        Weixin(4),

        /**
         * 短信
         */
        SMS(8);

        private int id;

        AlertType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static AlertType resolve(int id) {
            for (AlertType alertType : values()) {
                if (alertType.getId() == id) {
                    return alertType;
                }
            }
            throw new IllegalArgumentException("unknown AlertType, id: " + id);
        }

        /**
         * 二进制位解析，如3代表，http回调/邮件
         *
         * @param mergeId
         * @return
         */
        public static List<AlertType> resolveAll(int mergeId) {
            List<AlertType> alertTypeList = new ArrayList<>();
            for (AlertType alertType : values()) {
                if ((alertType.getId() & mergeId) != 0) {
                    alertTypeList.add(alertType);
                }
            }

            return alertTypeList;
        }
    }
}
