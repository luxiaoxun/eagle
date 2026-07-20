package com.alarm.eagle.cep.example;

import java.io.Serializable;

/**
 * 用户行为事件 — Dynamic CEP 测试用事件类
 *
 * <p>模拟用户操作：登录、浏览商品、下单、支付、转账等。
 */
public class UserBehaviorEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;
    /** 行为类型: LOGIN_SUCCESS, LOGIN_FAIL, VIEW_PRODUCT, PLACE_ORDER,
     *            PAYMENT_SUCCESS, PAYMENT_FAIL, CHANGE_PASSWORD, TRANSFER */
    private String action;
    /** 商品/产品 ID */
    private String product;
    /** 金额 */
    private double amount;
    /** 失败原因: BALANCE_NOT_ENOUGH, TIMEOUT, null */
    private String reason;
    /** 事件时间戳 (ms) */
    private long eventTime;

    public UserBehaviorEvent() {}

    public UserBehaviorEvent(String userId, String action, String product,
                             double amount, String reason, long eventTime) {
        this.userId = userId;
        this.action = action;
        this.product = product;
        this.amount = amount;
        this.reason = reason;
        this.eventTime = eventTime;
    }

    // Getters — Aviator 表达式通过 getter 访问字段
    public String getUserId() { return userId; }
    public String getAction() { return action; }
    public String getProduct() { return product; }
    public double getAmount() { return amount; }
    public String getReason() { return reason; }
    public long getEventTime() { return eventTime; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setAction(String action) { this.action = action; }
    public void setProduct(String product) { this.product = product; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setReason(String reason) { this.reason = reason; }
    public void setEventTime(long eventTime) { this.eventTime = eventTime; }

    @Override
    public String toString() {
        return "UserBehavior{userId='" + userId + "', action='" + action
                + "', product='" + product + "', amount=" + amount
                + ", reason='" + reason + "', time=" + eventTime + '}';
    }
}
