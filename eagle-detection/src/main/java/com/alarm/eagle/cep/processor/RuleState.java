package com.alarm.eagle.cep.processor;

/**
 * Rule lifecycle state.
 */
public enum RuleState {
    /** Rule is active and processing events */
    ACTIVE,
    /** Rule is paused (NFA preserved, but no longer accepts new events) */
    PAUSED,
    /** Rule is draining (no longer accepts new events, waiting for existing partial matches to timeout) */
    DRAINING,
    /** Rule is stopped */
    STOPPED,
    /** Rule is in error state */
    ERROR
}
