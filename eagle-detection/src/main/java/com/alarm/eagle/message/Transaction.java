package com.alarm.eagle.message;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * 交易事件模型 — Dynamic CEP 匹配用事件类
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private long transactionId;
    private long eventTime;
    private long payerId;
    private long beneficiaryId;
    private BigDecimal paymentAmount;
    private String paymentType;
    private Long ingestionTimestamp;

    private static final transient DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withLocale(Locale.US).withZone(ZoneOffset.UTC);

    public Transaction() {}

    /** 从 CSV 行解析: transactionId,eventTime,payerId,beneficiaryId,paymentType,paymentAmount,ingestionTimestamp */
    public static Transaction fromString(String line) {
        List<String> tokens = Arrays.asList(line.split(","));
        if (tokens.size() != 7) {
            throw new RuntimeException(
                    "Invalid transaction: " + line
                            + ". Required 7 fields, found " + tokens.size());
        }

        Transaction transaction = new Transaction();
        try {
            Iterator<String> iter = tokens.iterator();
            transaction.transactionId = Long.parseLong(iter.next());
            transaction.eventTime = ZonedDateTime.parse(iter.next(), timeFormatter)
                    .toInstant().toEpochMilli();
            transaction.payerId = Long.parseLong(iter.next());
            transaction.beneficiaryId = Long.parseLong(iter.next());
            transaction.paymentType = iter.next();
            transaction.paymentAmount = new BigDecimal(iter.next());
            transaction.ingestionTimestamp = Long.parseLong(iter.next());
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("Invalid record: " + line, nfe);
        }
        return transaction;
    }

    // ======================== Getters (Aviator 表达式通过 getter 访问) ========================

    public long getTransactionId() { return transactionId; }
    public long getEventTime() { return eventTime; }
    public long getPayerId() { return payerId; }
    public long getBeneficiaryId() { return beneficiaryId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public String getPaymentType() { return paymentType; }
    public Long getIngestionTimestamp() { return ingestionTimestamp; }

    // ======================== Setters ========================

    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }
    public void setEventTime(long eventTime) { this.eventTime = eventTime; }
    public void setPayerId(long payerId) { this.payerId = payerId; }
    public void setBeneficiaryId(long beneficiaryId) { this.beneficiaryId = beneficiaryId; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public void setIngestionTimestamp(Long ingestionTimestamp) { this.ingestionTimestamp = ingestionTimestamp; }

    @Override
    public String toString() {
        return "Transaction{txnId=" + transactionId
                + ", payer=" + payerId
                + ", beneficiary=" + beneficiaryId
                + ", amount=" + paymentAmount
                + ", type=" + paymentType + '}';
    }
}
