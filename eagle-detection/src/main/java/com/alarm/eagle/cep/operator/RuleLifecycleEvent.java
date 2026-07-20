package com.alarm.eagle.cep.operator;

import org.apache.flink.runtime.operators.coordination.OperatorEvent;

import java.io.Serializable;
import java.util.Objects;

/**
 * Rule lifecycle control event — triggered by external systems (e.g. REST API),
 * forwarded via Coordinator to TM side for execution.
 *
 * <p>Supported operations:
 * <ul>
 *   <li>{@code PAUSE} — Pause rule (NFA preserved, but no longer accepts new events)</li>
 *   <li>{@code RESUME} — Resume paused rule</li>
 *   <li>{@code DRAIN} — Graceful drain (no longer accepts new events, waiting for existing partial matches to timeout)</li>
 *   <li>{@code DELETE} — Force offline (immediately clean up NFA and all state)</li>
 * </ul>
 */
public class RuleLifecycleEvent implements OperatorEvent, Serializable {

    private static final long serialVersionUID = 1L;

    public enum Action {
        PAUSE,
        RESUME,
        DRAIN,
        DELETE
    }

    private final Action action;
    private final String ruleId;

    public RuleLifecycleEvent(Action action, String ruleId) {
        this.action = Objects.requireNonNull(action, "action");
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId");
    }

    public Action getAction() { return action; }
    public String getRuleId() { return ruleId; }

    @Override
    public String toString() {
        return "RuleLifecycleEvent{" + action + " " + ruleId + "}";
    }
}
