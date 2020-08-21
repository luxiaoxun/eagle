package com.alarm.eagle.rule;

import java.math.BigDecimal;

import com.alarm.eagle.accumulators.AverageAccumulator;
import com.alarm.eagle.accumulators.BigDecimalCounter;
import com.alarm.eagle.accumulators.BigDecimalMaximum;
import com.alarm.eagle.accumulators.BigDecimalMinimum;
import org.apache.flink.api.common.accumulators.SimpleAccumulator;

public class RuleHelper {

    /* Picks and returns a new accumulator, based on the Rule's aggregator function type. */
    public static SimpleAccumulator<BigDecimal> getAggregator(Rule rule) {
        switch (rule.getAggregatorFunctionType()) {
            case SUM:
                return new BigDecimalCounter();
            case AVG:
                return new AverageAccumulator();
            case MAX:
                return new BigDecimalMaximum();
            case MIN:
                return new BigDecimalMinimum();
            default:
                throw new RuntimeException("Unsupported aggregation function type: " + rule.getAggregatorFunctionType());
        }
    }
}
