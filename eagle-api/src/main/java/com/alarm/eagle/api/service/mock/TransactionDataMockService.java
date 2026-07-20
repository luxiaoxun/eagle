package com.alarm.eagle.api.service.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 交易数据 Mock 服务 — 生成随机 Transaction CSV 发送到 Kafka
 *
 * <p>Transaction CSV 格式:
 * transactionId,eventTime,payerId,beneficiaryId,paymentType,paymentAmount,ingestionTimestamp
 *
 * <p>通过 POST /mock/transaction 触发。
 * 仅 dev 环境可用。
 */
@Service
@Slf4j
@Profile("dev")
public class TransactionDataMockService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.transaction.topic:eagle-transaction}")
    private String topic;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /** 支付类型 */
    private static final String[] PAYMENT_TYPES = {"CSH", "CRD"};

    /** payer 池大小 */
    private static final int PAYER_POOL_SIZE = 50;

    /** beneficiary 池大小 */
    private static final int BENEFICIARY_POOL_SIZE = 100;

    // ======================== 单条发送 ========================

    @Async
    public void sendSingleTransaction() {
        String csv = generateTransactionCsv();
        sendToKafka(csv);
    }

    // ======================== 批量发送 ========================

    @Async
    public void sendBatchTransactions(int threadNum, int countPerThread,
                                       int intervalMs, int largeAmountPercent) {
        AtomicInteger totalCount = new AtomicInteger(0);

        Thread[] threads = new Thread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < countPerThread; j++) {
                    try {
                        // 按比例生成大额交易
                        String csv;
                        if (randomInt(1, 100) <= largeAmountPercent) {
                            csv = generateLargeTransactionCsv();
                        } else {
                            csv = generateTransactionCsv();
                        }
                        sendToKafka(csv);
                        Thread.sleep(intervalMs);
                        log.debug("Sent transaction count: {}", totalCount.incrementAndGet());
                    } catch (Exception e) {
                        log.error("Error sending transaction: ", e);
                    }
                }
            });
            threads[i].start();
        }
        log.info("Started {} threads, each sending {} transactions, interval {}ms",
                threadNum, countPerThread, intervalMs);
    }

    // ======================== 数据生成 ========================

    /** 生成普通交易 CSV */
    private String generateTransactionCsv() {
        long now = System.currentTimeMillis();
        long txnId = now * 1000 + randomInt(0, 999);

        long payerId = randomPayerId();
        long beneficiaryId = randomBeneficiaryId(payerId);
        String paymentType = PAYMENT_TYPES[randomInt(0, 1)];
        BigDecimal amount = randomAmount(10, 50000);
        String eventTime = DATE_FORMAT.format(new Date(now));

        return String.format("%d,%s,%d,%d,%s,%s,%d",
                txnId, eventTime, payerId, beneficiaryId,
                paymentType, amount.toPlainString(), now);
    }

    /** 生成大额交易 CSV（触发告警规则用） */
    private String generateLargeTransactionCsv() {
        long now = System.currentTimeMillis();
        long txnId = now * 1000 + randomInt(0, 999);

        // 从较小的 payer 池中选择，提高同一 payer 触发多笔规则的几率
        long payerId = randomInt(1, 5);      // 仅 1-5 的 payer 产生大额
        long beneficiaryId = randomBeneficiaryId(payerId);
        String paymentType = PAYMENT_TYPES[randomInt(0, 1)];
        // 大额：50,000 ~ 500,000
        BigDecimal amount = randomAmount(50000, 500000);
        String eventTime = DATE_FORMAT.format(new Date(now));

        return String.format("%d,%s,%d,%d,%s,%s,%d",
                txnId, eventTime, payerId, beneficiaryId,
                paymentType, amount.toPlainString(), now);
    }

    /** 生成小额测试后大额转账的场景数据（连续两条：小额+大额，同一 payer） */
    public void sendSmallThenLargePair(long payerId) {
        long now = System.currentTimeMillis();

        // 小额测试交易
        long txnId1 = now * 1000 + randomInt(0, 499);
        String csv1 = String.format("%d,%s,%d,%d,%s,%s,%d",
                txnId1,
                DATE_FORMAT.format(new Date(now)),
                payerId,
                randomBeneficiaryId(payerId),
                "CRD",
                randomAmount(1, 99).toPlainString(),   // < 100
                now);
        sendToKafka(csv1);

        // 大额转账（30 秒后）
        long txnId2 = txnId1 + 1;
        String csv2 = String.format("%d,%s,%d,%d,%s,%s,%d",
                txnId2,
                DATE_FORMAT.format(new Date(now + 30_000)),  // +30s
                payerId,
                randomBeneficiaryId(payerId),
                "CRD",
                randomAmount(200000, 500000).toPlainString(), // > 200000
                now + 30_000);
        sendToKafka(csv2);

        log.info("Sent small-then-large pair for payer={}", payerId);
    }

    // ======================== Kafka 发送 ========================

    private void sendToKafka(String csv) {
        ListenableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(topic, csv);
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.debug("Sent transaction to {}: {}", topic, csv);
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("Failed to send transaction: {}", csv, ex);
            }
        });
    }

    // ======================== 随机工具方法 ========================

    private static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static long randomPayerId() {
        return randomInt(1, PAYER_POOL_SIZE);
    }

    private static long randomBeneficiaryId(long excludePayerId) {
        long id;
        do {
            id = randomInt(1, BENEFICIARY_POOL_SIZE);
        } while (id == excludePayerId);
        return id;
    }

    private static BigDecimal randomAmount(int min, int max) {
        double value = min + ThreadLocalRandom.current().nextDouble() * (max - min);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
