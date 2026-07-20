package com.alarm.eagle.cep.operator;

import com.alarm.eagle.cep.processor.PatternProcessorDiscovererFactory;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.operators.coordination.OperatorCoordinator;

/**
 * Coordinator Provider — creates a {@link DynamicCepCoordinator} instance on the JM side.
 */
public class DynamicCepCoordinatorProvider<IN>
        implements OperatorCoordinator.Provider {

    private static final long serialVersionUID = 1L;

    private final String operatorName;
    private final OperatorID operatorID;
    private final PatternProcessorDiscovererFactory<IN> discovererFactory;

    public DynamicCepCoordinatorProvider(
            String operatorName,
            OperatorID operatorID,
            PatternProcessorDiscovererFactory<IN> discovererFactory) {
        this.operatorName = operatorName;
        this.operatorID = operatorID;
        this.discovererFactory = discovererFactory;
    }

    @Override
    public OperatorID getOperatorId() {
        return operatorID;
    }

    @Override
    public OperatorCoordinator create(
            OperatorCoordinator.Context context) {
        return new DynamicCepCoordinator<>(
                operatorName, discovererFactory, context);
    }
}
