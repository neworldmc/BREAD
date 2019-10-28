/*
 * Copyright (C) 2019 Sunbread.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.sunbread.bread.common;

import top.sunbread.bread.common.BREADStatistics.ClusterStatistics;
import top.sunbread.bread.common.BREADStatistics.NoiseStatistics;
import top.sunbread.bread.common.BREADStatistics.Point;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A util class for analysing game data.
 */
final class BREADAnalysis {

    private static final int EPSILON = 16;
    private static final int MAX_WEIGHT_SUM_FAST = 25; // For noise
    private static final int MAX_WEIGHT_SUM = 100; // For noise

    /**
     * Analyse given points and divide them to clusters and noise.
     * This uses parallel DBSCAN.
     *
     * @param points Set of points to analyse
     * @param fast   Fast mode of BREAD
     * @return A list of clusters and noise, the last element of the list is noise (guarantee existence)
     */
    static List<Set<Point>> clusterAnalysis(Set<Point> points, boolean fast) {
        ForkJoinPool analysisThreadPool = new ForkJoinPool();
        List<Set<Point>> outerResult = analysisThreadPool.submit(() -> {
            // Wrapping points
            Set<PointData> pointsData = points.parallelStream().unordered().
                    map(PointData::new).collect(Collectors.toSet());

            // Constructing Range Tree
            RangeTree<PointData> rangeTree = new RangeTree<>(pointsData,
                    Arrays.asList(p -> p.point.x, p -> p.point.y, p -> p.point.z));

            // Dyeing core points
            pointsData.parallelStream().unordered().filter(pointData ->
                    rangeTree.getNeighborPointsManhattan(pointData, EPSILON).parallelStream().unordered().
                            mapToInt(neighborPointData -> neighborPointData.point.w).sum() >
                            (fast ? MAX_WEIGHT_SUM_FAST : MAX_WEIGHT_SUM)).
                    forEach(corePointData -> corePointData.pointAttribute = PointAttribute.CORE);

            // Collecting core points
            Set<PointData> corePointsData = pointsData.parallelStream().unordered().
                    filter(pointData -> pointData.pointAttribute == PointAttribute.CORE).
                    collect(Collectors.toSet());

            // Constructing Union-Collect Set (a variant of Union-Find Set) of core points
            UnionCollect<PointData> clustersUnionCollect = new UnionCollect<>(corePointsData);

            // Merging core points
            corePointsData.parallelStream().unordered().forEach(corePointData ->
                    rangeTree.getNeighborPointsManhattan(corePointData, EPSILON).parallelStream().unordered().
                            filter(pointData -> pointData.pointAttribute == PointAttribute.CORE).
                            forEach(neighborPointData ->
                                    clustersUnionCollect.union(corePointData, neighborPointData)));
            corePointsData.clear();

            // Collecting core points from Union-Collect Set
            List<Set<PointData>> clusters = new ArrayList<>(clustersUnionCollect.collect());
            clustersUnionCollect.clear();

            // Numbering core points according to clusters
            IntStream.range(0, clusters.size()).parallel().unordered().forEach(index ->
                    clusters.get(index).parallelStream().unordered().forEach(pointData ->
                            pointData.extraData = index + 1));
            clusters.clear();

            // Dyeing and numbering reachable points
            BiFunction<PointData, PointData, Integer> manhattanDistance =
                    (pd1, pd2) -> Math.abs(pd1.point.x - pd2.point.x) +
                            Math.abs(pd1.point.y - pd2.point.y) + Math.abs(pd1.point.z - pd2.point.z);
            pointsData.parallelStream().unordered().filter(pointData ->
                    pointData.pointAttribute == PointAttribute.NONE).
                    forEach(pointData ->
                            rangeTree.getNeighborPointsManhattan(pointData, EPSILON).parallelStream().unordered().
                                    filter(neighborPointData -> neighborPointData.pointAttribute == PointAttribute.CORE).
                                    min(Comparator.comparing(corePointData ->
                                            manhattanDistance.apply(pointData, corePointData))).
                                    ifPresent(nearestCorePointData -> {
                                        pointData.pointAttribute = PointAttribute.REACHABLE;
                                        pointData.extraData = nearestCorePointData.extraData;
                                    }));
            rangeTree.clear();

            // Collecting points
            List<Set<Point>> result = new ArrayList<>(pointsData.parallelStream().unordered().
                    filter(pointData -> pointData.pointAttribute != PointAttribute.NONE).
                    collect(Collectors.toMap(pointData -> pointData.extraData,
                            pointData -> Collections.singleton(pointData.point),
                            (set1, set2) -> Stream.concat(set1.stream(), set2.stream()).collect(Collectors.toSet()))).
                    values());
            result.add(pointsData.parallelStream().unordered().
                    filter(pointData -> pointData.pointAttribute == PointAttribute.NONE).
                    map(pointData -> pointData.point).collect(Collectors.toSet()));
            pointsData.clear();

            // Return results
            return result;
        }).join();
        analysisThreadPool.shutdownNow();
        return outerResult;
    }

    /**
     * Count a cluster and generate its statistics.
     *
     * @param cluster Cluster to count
     * @param fast    Fast mode of BREAD
     * @return Statistics of the given cluster
     */
    static ClusterStatistics countCluster(Set<Point> cluster, boolean fast) {
        int totalEvents = cluster.parallelStream().mapToInt(point -> point.w).sum();
        double eventsPerTick = (double) totalEvents /
                (fast ? BREADAnalyser.FAST_COLLECTING_TICKS : BREADAnalyser.COLLECTING_TICKS);
        double[] eventCentroidLocation = cluster.parallelStream().
                map(point -> Arrays.asList(BigInteger.valueOf((long) point.x * point.w),
                        BigInteger.valueOf((long) point.y * point.w),
                        BigInteger.valueOf((long) point.z * point.w))).
                reduce(Arrays.asList(BigInteger.valueOf(0), BigInteger.valueOf(0), BigInteger.valueOf(0)),
                        (list1, list2) -> IntStream.range(0, list1.size()).parallel().boxed().
                                map(i -> list1.get(i).add(list2.get(i))).collect(Collectors.toList())).
                parallelStream().mapToDouble(bigInt -> new BigDecimal(bigInt).
                divide(BigDecimal.valueOf(totalEvents), 16, RoundingMode.HALF_UP).doubleValue()).
                toArray();
        double distanceFromCentroid = cluster.parallelStream().
                mapToDouble(point -> Math.sqrt(Math.pow(point.x - eventCentroidLocation[0], 2) +
                        Math.pow(point.y - eventCentroidLocation[1], 2) +
                        Math.pow(point.z - eventCentroidLocation[2], 2)) * point.w).
                sum() / totalEvents;
        return new ClusterStatistics(cluster, eventsPerTick,
                eventCentroidLocation, distanceFromCentroid);
    }

    /**
     * Count noise and generate its statistics.
     *
     * @param noise Noise to count
     * @param fast  Fast mode of BREAD
     * @return Statistics of the given noise
     */
    static NoiseStatistics countNoise(Set<Point> noise, boolean fast) {
        return new NoiseStatistics(noise, (double) noise.parallelStream().
                mapToInt(point -> point.w).sum() /
                (fast ? BREADAnalyser.FAST_COLLECTING_TICKS : BREADAnalyser.COLLECTING_TICKS));
    }

    /**
     * A point wrapper.
     */
    private static final class PointData {

        Point point;
        PointAttribute pointAttribute;
        int extraData;

        PointData(Point point) {
            this.point = point;
            this.pointAttribute = PointAttribute.NONE;
            this.extraData = 0;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (this == o) return true;
            if (!(o instanceof PointData)) return false;
            return this.point.equals(((PointData) o).point) &&
                    this.pointAttribute.equals(((PointData) o).pointAttribute) &&
                    this.extraData == ((PointData) o).extraData;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.point, this.pointAttribute, this.extraData);
        }

    }

    /**
     * An implementation of Range Tree.
     * It's used to get neighbor points of a specific point efficiently.
     *
     * @param <P> Point type
     * @see <a href="https://en.wikipedia.org/wiki/Range_tree">Wikipedia</a>
     */
    private static final class RangeTree<P> {

        private List<Function<P, Integer>> componentFilters;
        private Node<P> tree;

        /**
         * Construct a Range Tree.
         *
         * @param points           Points as elements
         * @param componentFilters A list of functions to get components of a point
         */
        RangeTree(Set<P> points, List<Function<P, Integer>> componentFilters) {
            this.componentFilters = new ArrayList<>(componentFilters);
            this.tree = makeTree(0, points);
        }

        /**
         * Get neighbor points of a specific point, using Manhattan distance.
         *
         * @param centerPoint Specific point
         * @param epsilon     Range (inclusive)
         * @return Neighbor points of the specific point (including center point if it's in the tree)
         */
        Set<P> getNeighborPointsManhattan(P centerPoint, int epsilon) {
            int realEps = Math.abs(epsilon);
            List<Integer> from = this.componentFilters.stream().map(f -> f.apply(centerPoint)).
                    map(c -> c - realEps).collect(Collectors.toList());
            List<Integer> to = this.componentFilters.stream().map(f -> f.apply(centerPoint)).
                    map(c -> c + realEps).collect(Collectors.toList());
            BiFunction<P, P, Integer> manhattanDistance =
                    (p1, p2) -> this.componentFilters.stream().mapToInt(d -> Math.abs(d.apply(p1) - d.apply(p2))).sum();
            return queryInclusiveRange(from, to).parallelStream().unordered().
                    filter(point -> manhattanDistance.apply(centerPoint, point) <= epsilon).
                    collect(Collectors.toSet());
        }

        /**
         * Remove all elements from this Range Tree.
         */
        void clear() {
            this.componentFilters.clear();
            clearTree(this.tree);
            this.tree = new Node<>((P) null);
        }

        /**
         * A recursive function to make Range Tree.
         *
         * @param depth  Depth of the node
         * @param points Elements of the node
         * @return Made point
         */
        private Node<P> makeTree(int depth, Set<P> points) {
            if (depth == this.componentFilters.size()) {
                if (points.size() == 1)
                    return new Node<>(points.iterator().next());
                else if (points.size() == 0) {
                    assert false;
                    throw new RuntimeException();
                } else
                    throw new RuntimeException("Found duplicate points");
            }
            Function<P, Integer> componentFilter = this.componentFilters.get(depth);
            Function<Integer, Node<P>> treeNodeGenerator =
                    component -> makeTree(depth + 1, points.parallelStream().unordered().filter(childPoint ->
                            componentFilter.apply(childPoint).equals(component)).collect(Collectors.toSet()));
            // Return type isn't Node<TreeMap> but Node<P>
            return new Node<>(points.parallelStream().unordered().map(componentFilter).distinct().
                    collect(Collectors.toMap(Function.identity(), treeNodeGenerator,
                            (v1, v2) -> {
                                assert false;
                                throw new RuntimeException();
                            }, TreeMap::new)));
        }

        /**
         * A recursive function to clear Range Tree.
         *
         * @param tree Node that will be removed
         */
        private void clearTree(Node<P> tree) {
            if (tree.isLeaf()) return;
            tree.getBST().values().parallelStream().unordered().forEach(this::clearTree);
            tree.getBST().clear();
        }

        /**
         * Get all points in a rectangle range (inclusive).
         *
         * @param from Vertex coordinates
         * @param to   Diagonal vertex coordinates
         * @return All points in a rectangle range (inclusive)
         */
        private Set<P> queryInclusiveRange(List<Integer> from, List<Integer> to) {
            if (from.size() != to.size() || from.size() != this.componentFilters.size())
                throw new IllegalArgumentException();
            Stack<Node<P>> nodeStack = new Stack<>();
            Stack<List<Node<P>>> childNodesStack = new Stack<>();
            Set<P> result = new HashSet<>();
            nodeStack.push(this.tree);
            childNodesStack.push(null);
            while (!nodeStack.empty()) {
                Node<P> node = nodeStack.peek();
                int depth = nodeStack.size() - 1;
                if (node.isLeaf()) {
                    result.add(node.getPoint());
                    nodeStack.pop();
                    childNodesStack.pop();
                } else {
                    List<Node<P>> childNodes = childNodesStack.peek();
                    if (childNodes == null) {
                        int less = Math.min(from.get(depth), to.get(depth));
                        int greater = Math.max(from.get(depth), to.get(depth));
                        childNodesStack.pop();
                        childNodesStack.push(new LinkedList<>(Objects.requireNonNull(node.getBST()).
                                subMap(less, true, greater, true).values()));
                        childNodes = childNodesStack.peek();
                    }
                    if (childNodes.isEmpty()) {
                        nodeStack.pop();
                        childNodesStack.pop();
                    } else {
                        Node<P> childNode = childNodes.get(0);
                        childNodes.remove(0);
                        nodeStack.push(childNode);
                        childNodesStack.push(null);
                    }
                }
            }
            return result;
        }

        /**
         * A wrapper that wraps trees and points.
         */
        private static final class Node<P> {

            private UUID uuid;
            private boolean leaf;
            private P point;
            private TreeMap<Integer, Node<P>> bst;

            Node(P point) {
                this.uuid = UUID.randomUUID();
                this.leaf = true;
                this.point = point;
            }

            Node(TreeMap<Integer, Node<P>> bst) {
                this.uuid = UUID.randomUUID();
                this.leaf = false;
                this.bst = bst;
            }

            boolean isLeaf() {
                return this.leaf;
            }

            P getPoint() {
                return this.leaf ? this.point : null;
            }

            TreeMap<Integer, Node<P>> getBST() {
                return this.leaf ? null : this.bst;
            }

            @Override
            public boolean equals(Object o) {
                if (o == null) return false;
                if (this == o) return true;
                if (!(o instanceof Node)) return false;
                return this.uuid.equals(((Node) o).uuid);
            }

        }

    }

    /**
     * An implementation of a derivative of Disjoint-Set.
     * It's used to union elements efficiently and collect the result.
     *
     * @param <E> Element type
     * @see <a href="https://en.wikipedia.org/wiki/Disjoint-set_data_structure">Wikipedia</a>
     */
    private static final class UnionCollect<E> {

        private List<E> nodes;
        private int[] parents;

        UnionCollect(Set<E> elements) {
            this.nodes = new ArrayList<>(elements);
            this.parents = IntStream.range(0, this.nodes.size()).toArray();
        }

        synchronized void union(E element1, E element2) {
            if (element1 == element2) return;
            if (!this.nodes.contains(element1) || !this.nodes.contains(element2)) return;
            Root root1 = getRoot(this.nodes.indexOf(element1));
            Root root2 = getRoot(this.nodes.indexOf(element2));
            if (root1.node == root2.node) return;
            if (root1.rank < root2.rank)
                this.parents[root1.node] = root2.node;
            else
                this.parents[root2.node] = root1.node;
        }

        Set<Set<E>> collect() {
            Map<Integer, Set<E>> collector = new LinkedHashMap<>();
            for (int node = 0; node < this.nodes.size(); ++node) {
                int root = getRoot(node).node;
                if (!collector.containsKey(root)) collector.put(root, new HashSet<>());
                collector.get(root).add(this.nodes.get(node));
            }
            return new HashSet<>(collector.values());
        }

        void clear() {
            this.nodes.clear();
            this.parents = new int[0];
        }

        private Root getRoot(int node) {
            int rank = 0;
            if (node != this.parents[node]) {
                Root root = getRoot(this.parents[node]);
                this.parents[node] = root.node;
                rank = root.rank + 1;
            }
            return new Root(this.parents[node], rank);
        }

        private static final class Root {

            int node;
            int rank;

            Root(int node, int rank) {
                this.node = node;
                this.rank = rank;
            }

        }

    }

    /**
     * The attribute of PointData.
     */
    private enum PointAttribute {NONE, REACHABLE, CORE}

}
