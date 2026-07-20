package com.alarm.eagle.cep.operator;

import com.alarm.eagle.cep.processor.PatternProcessor;
import org.apache.flink.runtime.operators.coordination.OperatorEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Incremental rule change event — pushes only changed rules, not the full list.
 *
 * <p>Unlike {@link DynamicCepOperator.UpdatePatternProcessorEvent} which performs full replacement,
 * this event carries only three types of changes:
 * <ul>
 *   <li>{@code added}   — newly launched rules (previously non-existent)</li>
 *   <li>{@code removed} — offline rules (id+version identifier only)</li>
 *   <li>{@code updated} — version-updated rules (same id, higher version)</li>
 * </ul>
 *
 * <p>TM side handles by type on receipt: added→create NFA, updated→hot swap, deleted→cleanup.
 * Active rules not appearing in any list remain untouched.
 *
 * @param <T> event type
 */
public class PatternProcessorChangeEvent<T>
        implements OperatorEvent, Serializable {

    private static final long serialVersionUID = 1L;

    /** Newly launched rules (added) */
    private final List<PatternProcessor<T>> added;

    /** Offline rule id+version (deleted) */
    private final List<RuleIdentifier> removed;

    /** Version-updated rules (replaced) */
    private final List<PatternProcessor<T>> updated;

    public PatternProcessorChangeEvent(
            List<PatternProcessor<T>> added,
            List<RuleIdentifier> removed,
            List<PatternProcessor<T>> updated) {
        this.added = (added != null) ? new ArrayList<>(added) : Collections.emptyList();
        this.removed = (removed != null) ? new ArrayList<>(removed) : Collections.emptyList();
        this.updated = (updated != null) ? new ArrayList<>(updated) : Collections.emptyList();
    }

    public List<PatternProcessor<T>> getAdded() {
        return added;
    }

    public List<RuleIdentifier> getRemoved() {
        return removed;
    }

    public List<PatternProcessor<T>> getUpdated() {
        return updated;
    }

    /** @return Whether there are any changes */
    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty() && updated.isEmpty();
    }

    @Override
    public String toString() {
        return "PatternProcessorChangeEvent{added=" + added.size()
                + ", removed=" + removed.size()
                + ", updated=" + updated.size() + '}';
    }

    /**
     * Short rule identifier — used in the removed list to identify rules to delete.
     */
    public static class RuleIdentifier implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String id;
        private final int version;

        public RuleIdentifier(String id, int version) {
            this.id = id;
            this.version = version;
        }

        public String getId() { return id; }
        public int getVersion() { return version; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RuleIdentifier)) return false;
            RuleIdentifier r = (RuleIdentifier) o;
            return version == r.version && id.equals(r.id);
        }

        @Override
        public int hashCode() {
            return 31 * id.hashCode() + version;
        }

        @Override
        public String toString() {
            return id + "-v" + version;
        }
    }
}
