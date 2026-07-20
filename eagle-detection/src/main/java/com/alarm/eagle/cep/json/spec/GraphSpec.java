package com.alarm.eagle.cep.json.spec;

import org.apache.flink.cep.nfa.aftermatch.AfterMatchSkipStrategy;
import org.apache.flink.cep.pattern.GroupPattern;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.Quantifier;
import org.apache.flink.cep.pattern.WithinType;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON representation of the complete Pattern DAG.
 *
 * <p>GraphSpec models a Flink Pattern chain as a directed graph:
 * <ul>
 *   <li><b>nodes</b> — each Pattern node in the graph</li>
 *   <li><b>edges</b> — transition relationships between nodes (next/followedBy/followedByAny)</li>
 *   <li><b>window</b> — global time window (corresponds to {@code within(Duration)})</li>
 *   <li><b>afterMatchStrategy</b> — After-match skip strategy</li>
 * </ul>
 */
public class GraphSpec extends NodeSpec {

    private final int version;
    private final List<NodeSpec> nodes;
    private final List<EdgeSpec> edges;
    private final WindowSpec window;
    private final AfterMatchSkipStrategySpec afterMatchStrategy;

    public GraphSpec() {
        super(null, new QuantifierSpec(), null, PatternNodeType.COMPOSITE);
        this.version = 1;
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.window = null;
        this.afterMatchStrategy = new AfterMatchSkipStrategySpec();
    }

    public GraphSpec(String name,
                     QuantifierSpec quantifier,
                     ConditionSpec condition,
                     List<NodeSpec> nodes,
                     List<EdgeSpec> edges,
                     WindowSpec window,
                     AfterMatchSkipStrategySpec afterMatchStrategy) {
        super(name, quantifier, condition, PatternNodeType.COMPOSITE);
        this.version = 1;
        this.nodes = nodes != null ? nodes : new ArrayList<>();
        this.edges = edges != null ? edges : new ArrayList<>();
        this.window = window;
        this.afterMatchStrategy = afterMatchStrategy != null
                ? afterMatchStrategy : new AfterMatchSkipStrategySpec();
    }

    // ======================== Getters ========================

    public int getVersion() {
        return version;
    }

    public List<NodeSpec> getNodes() {
        return nodes;
    }

    public List<EdgeSpec> getEdges() {
        return edges;
    }

    public WindowSpec getWindow() {
        return window;
    }

    public AfterMatchSkipStrategySpec getAfterMatchStrategy() {
        return afterMatchStrategy;
    }

    // ======================== Pattern → GraphSpec ========================

    /**
     * ★ Construct GraphSpec from a Flink Pattern.
     * Traverse the Pattern linked list from back to front, collect nodes and edges, then reverse.
     */
    public static GraphSpec fromPattern(Pattern<?, ?> pattern) {
        List<NodeSpec> nodeList = new ArrayList<>();
        List<EdgeSpec> edgeList = new ArrayList<>();

        // Traverse Pattern chain from back to front
        Pattern<?, ?> current = pattern;
        while (current != null) {
            if (current instanceof GroupPattern) {
                GraphSpec sub = GraphSpec.fromPattern(
                        ((GroupPattern) current).getRawPattern());
                nodeList.add(sub);
            } else {
                nodeList.add(NodeSpec.fromPattern(current));
            }

            if (current.getPrevious() != null) {
                String srcName = resolveName(current.getPrevious());
                String tgtName = resolveName(current);
                edgeList.add(new EdgeSpec(srcName, tgtName,
                        current.getQuantifier().getConsumingStrategy()));
            }

            current = current.getPrevious();
        }

        // Reverse (since collected from back to front)
        Collections.reverse(nodeList);
        Collections.reverse(edgeList);

        // Collect window
        WindowSpec winSpec = null;
        if (pattern.getWindowTime(WithinType.FIRST_AND_LAST) != null) {
            winSpec = WindowSpec.fromWindowTime(
                    WithinType.FIRST_AND_LAST,
                    pattern.getWindowTime(WithinType.FIRST_AND_LAST));
        } else if (pattern.getWindowTime(WithinType.PREVIOUS_AND_CURRENT) != null) {
            winSpec = WindowSpec.fromWindowTime(
                    WithinType.PREVIOUS_AND_CURRENT,
                    pattern.getWindowTime(WithinType.PREVIOUS_AND_CURRENT));
        }

        QuantifierSpec q = new QuantifierSpec(
                pattern.getQuantifier(),
                pattern.getTimes(),
                pattern.getUntilCondition());

        AfterMatchSkipStrategySpec ams = AfterMatchSkipStrategySpec
                .fromAfterMatchSkipStrategy(
                        pattern.getAfterMatchSkipStrategy());

        return new GraphSpec(
                resolveName(pattern), q, null,
                nodeList, edgeList, winSpec, ams);
    }

    // ======================== GraphSpec → Pattern ========================

    /**
     * ★★ Core method: reconstruct a complete Flink Pattern object from GraphSpec.
     *
     * @param classLoader user code ClassLoader, for loading Class-type conditions
     * @return Complete Pattern chain (Pattern of the last node, containing the previous linked list)
     */
    public Pattern<?, ?> toPattern(ClassLoader classLoader) throws Exception {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("GraphSpec has no nodes.");
        }

        // ① Build index
        Map<String, NodeSpec> nodeCache = new HashMap<>();
        for (NodeSpec ns : nodes) {
            nodeCache.put(ns.getName(), ns);
        }

        Map<String, EdgeSpec> edgeCache = new HashMap<>();
        for (EdgeSpec es : edges) {
            edgeCache.put(es.getSource(), es);
        }

        // ② Find start node (node with in-degree 0)
        String beginNodeName = findBeginPatternName();

        AfterMatchSkipStrategy strategy = afterMatchStrategy != null
                ? afterMatchStrategy.toAfterMatchSkipStrategy()
                : AfterMatchSkipStrategy.noSkip();

        // ③ Traverse from start node along edges, building Pattern chain node by node
        Pattern<?, ?> prevPattern = null;
        String prevNodeName = null;
        String currentNodeName = beginNodeName;

        while (currentNodeName != null) {
            NodeSpec currentNodeSpec = nodeCache.get(currentNodeName);
            if (currentNodeSpec == null) {
                throw new IllegalStateException(
                        "Node '" + currentNodeName + "' not found in nodes list.");
            }

            EdgeSpec edgeToCurrentNode = edgeCache.get(prevNodeName);

            Quantifier.ConsumingStrategy edgeType = prevNodeName == null
                    ? Quantifier.ConsumingStrategy.STRICT
                    : edgeToCurrentNode.getType();

            Pattern<?, ?> currentPattern = currentNodeSpec.toPattern(
                    prevPattern, strategy, edgeType, classLoader);

            // If nested GraphSpec (COMPOSITE), wrap with GroupPattern
            if (currentNodeSpec instanceof GraphSpec
                    && !(currentNodeSpec instanceof GraphSpec && prevNodeName == null)) {
                if (prevPattern != null) {
                    currentPattern = buildGroupConnection(
                            edgeType, currentPattern, prevPattern);
                }
            }

            prevPattern = currentPattern;
            prevNodeName = currentNodeName;
            currentNodeName = edgeCache.get(currentNodeName) != null
                    ? edgeCache.get(currentNodeName).getTarget()
                    : null;
        }

        // ④ Set global time window
        if (window != null && prevPattern != null) {
            prevPattern.within(window.getTime(), window.getType());
        }

        return prevPattern;
    }

    // ======================== Internal Helper Methods ========================

    /**
     * Find the node with in-degree 0 — the start of the Pattern chain.
     */
    private String findBeginPatternName() {
        Set<String> nodeNames = new HashSet<>();
        for (NodeSpec node : nodes) {
            nodeNames.add(node.getName());
        }
        // Remove all nodes pointed to by edges (targets)
        for (EdgeSpec edge : edges) {
            nodeNames.remove(edge.getTarget());
        }
        if (nodeNames.size() != 1) {
            throw new IllegalStateException(
                    "There must be exactly one begin node (indegree=0), but found "
                            + nodeNames.size() + ": " + nodeNames);
        }
        return nodeNames.iterator().next();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Pattern<?, ?> buildGroupConnection(
            Quantifier.ConsumingStrategy strategy,
            Pattern<?, ?> currentPattern,
            Pattern<?, ?> prevPattern) {
        Pattern rawPrev = prevPattern;
        Pattern rawCurr = currentPattern;
        switch (strategy) {
            case STRICT:
                return rawPrev.next(rawCurr);
            case SKIP_TILL_NEXT:
                return rawPrev.followedBy(rawCurr);
            case SKIP_TILL_ANY:
                return rawPrev.followedByAny(rawCurr);
            default:
                return rawPrev.followedBy(rawCurr);
        }
    }

    /**
     * Resolve the name of a Pattern. For GroupPattern, return the name of its inner RawPattern.
     */
    private static String resolveName(Pattern<?, ?> pattern) {
        if (pattern instanceof GroupPattern) {
            return ((GroupPattern) pattern).getRawPattern().getName();
        }
        return pattern.getName();
    }

    @Override
    public String toString() {
        return "GraphSpec{name='" + getName()
                + "', nodes=" + nodes.size()
                + ", edges=" + edges.size()
                + ", window=" + window + '}';
    }
}
