package com.alarm.eagle.cep.operator;

import com.alarm.eagle.cep.processor.PatternProcessorDiscovererFactory;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.operators.coordination.OperatorCoordinator;
import org.apache.flink.streaming.api.operators.AbstractStreamOperatorFactory;
import org.apache.flink.streaming.api.operators.CoordinatedOperatorFactory;
import org.apache.flink.streaming.api.operators.OneInputStreamOperatorFactory;
import org.apache.flink.streaming.api.operators.StreamOperator;
import org.apache.flink.streaming.api.operators.StreamOperatorParameters;
import org.apache.flink.util.Preconditions;

/**
 * Factory class for DynamicCepOperator.
 *
 * <p>Implements {@link CoordinatedOperatorFactory}, supporting JM-side Coordinator and
 * TM-side Operator OperatorEvent communication.
 */
public class DynamicCepOperatorFactory<IN, OUT>
        extends AbstractStreamOperatorFactory<OUT>
        implements OneInputStreamOperatorFactory<IN, OUT>,
                   CoordinatedOperatorFactory<OUT> {

    private static final long serialVersionUID = 1L;

    private final PatternProcessorDiscovererFactory<IN> discovererFactory;
    private final TypeSerializer<IN> inputSerializer;
    private final boolean isProcessingTime;

    public DynamicCepOperatorFactory(
            PatternProcessorDiscovererFactory<IN> discovererFactory,
            TypeSerializer<IN> inputSerializer,
            boolean isProcessingTime) {
        this.discovererFactory = Preconditions.checkNotNull(discovererFactory);
        this.inputSerializer = Preconditions.checkNotNull(inputSerializer);
        this.isProcessingTime = isProcessingTime;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends StreamOperator<OUT>> T createStreamOperator(
            StreamOperatorParameters<OUT> parameters) {
        OperatorID operatorId =
                parameters.getStreamConfig().getOperatorID();

        DynamicCepOperator<IN, ?, OUT> operator =
                new DynamicCepOperator<>(inputSerializer, isProcessingTime);

        operator.setup(
                parameters.getContainingTask(),
                parameters.getStreamConfig(),
                parameters.getOutput());

        // ★ Explicitly set ProcessingTimeService (AbstractStreamOperator V1 does not auto-inject)
        operator.setProcessingTimeService(
                parameters.getProcessingTimeService());

        // ★ Register as OperatorEventHandler to receive Coordinator events
        parameters.getOperatorEventDispatcher()
                .registerEventHandler(operatorId, operator);

        return (T) operator;
    }

    @Override
    public OperatorCoordinator.Provider getCoordinatorProvider(
            String operatorName, OperatorID operatorID) {
        return new DynamicCepCoordinatorProvider<>(
                operatorName, operatorID, discovererFactory);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<? extends StreamOperator> getStreamOperatorClass(
            ClassLoader classLoader) {
        return DynamicCepOperator.class;
    }
}
