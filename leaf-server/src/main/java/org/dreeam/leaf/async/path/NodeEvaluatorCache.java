package org.dreeam.leaf.async.path;

import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class NodeEvaluatorCache {

    private static final ConcurrentHashMap<Integer, ConcurrentLinkedDeque<NodeEvaluator>> NODE_EVALUATORS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<NodeEvaluator, NodeEvaluatorGenerator> NODE_EVALUATOR_TO_GENERATOR = new ConcurrentHashMap<>();

    public static final ThreadLocal<BinaryHeap> HEAP_LOCAL = ThreadLocal.withInitial(BinaryHeap::new);
    public static final ThreadLocal<Node[]> NEIGHBORS_LOCAL = ThreadLocal.withInitial(() -> new Node[32]);

    private NodeEvaluatorCache() {
    }

    public static NodeEvaluator takeNodeEvaluator(NodeEvaluatorGenerator generator, NodeEvaluator localNodeEvaluator) {
        final int nodeEvaluatorFeatures = NodeEvaluatorFeatures.fromNodeEvaluator(localNodeEvaluator);
        final ConcurrentLinkedDeque<NodeEvaluator> evaluators = NODE_EVALUATORS.computeIfAbsent(nodeEvaluatorFeatures, _ -> new ConcurrentLinkedDeque<>());
        NodeEvaluator nodeEvaluator = evaluators.pollFirst();

        if (nodeEvaluator == null) {
            nodeEvaluator = generator.generate(NodeEvaluatorFeatures.unpack(nodeEvaluatorFeatures));
        }

        NODE_EVALUATOR_TO_GENERATOR.put(nodeEvaluator, generator);

        return nodeEvaluator;
    }

    public static void returnNodeEvaluator(final NodeEvaluator nodeEvaluator) {
        final NodeEvaluatorGenerator generator = NODE_EVALUATOR_TO_GENERATOR.remove(nodeEvaluator);
        Validate.notNull(generator, "NodeEvaluator already returned");

        final int feature = NodeEvaluatorFeatures.fromNodeEvaluator(nodeEvaluator);
        NODE_EVALUATORS.computeIfAbsent(feature, _ -> new ConcurrentLinkedDeque<>()).offerFirst(nodeEvaluator);
    }

    public static void removeNodeEvaluator(final NodeEvaluator nodeEvaluator) {
        NODE_EVALUATOR_TO_GENERATOR.remove(nodeEvaluator);
    }
}
