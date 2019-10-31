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
import top.sunbread.bread.common.BREADStatistics.WorldStatistics;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A process for analysing collected redstone data.
 */
public final class BREADAnalyser {

    public static final int COLLECTING_TICKS_BASE = 300;
    private static final int THREADS = 16;
    private static final int TIMEOUT_MINUTES = 1;

    private volatile ExecutorService threadPool;
    private CompletableFuture<Void> future;

    /**
     * Initialize a process for analysing collected redstone data.
     *
     * @param points                     Redstone event data
     * @param collectionPeriodMultiplier Collection period multiplier, must be a positive integer,
     *                                   the base value of collection period is 15 seconds (300 ticks)
     * @param asyncCallback              Async callback for transferring the result
     */
    public BREADAnalyser(Map<UUID, Set<Point>> points, int collectionPeriodMultiplier,
                         Consumer<Optional<Map<UUID, WorldStatistics>>> asyncCallback) {
        this.threadPool = Executors.newFixedThreadPool(THREADS);
        Function<Set<Point>, CompletableFuture<List<Set<Point>>>> clusterAnalysisProvider =
                p -> CompletableFuture.supplyAsync(() ->
                        BREADAnalysis.clusterAnalysis(p, collectionPeriodMultiplier), this.threadPool);
        Function<Set<Point>, CompletableFuture<ClusterStatistics>> countClusterProvider =
                c -> CompletableFuture.supplyAsync(() -> BREADAnalysis.countCluster(c,
                        COLLECTING_TICKS_BASE * collectionPeriodMultiplier), this.threadPool);
        Function<Set<Point>, CompletableFuture<NoiseStatistics>> countNoiseProvider =
                n -> CompletableFuture.supplyAsync(() -> BREADAnalysis.countNoise(n,
                        COLLECTING_TICKS_BASE * collectionPeriodMultiplier), this.threadPool);
        Function<Set<Point>, CompletableFuture<WorldStatistics>> worldStatsAnalysis =
                p -> clusterAnalysisProvider.apply(p).thenCompose(analysis -> {
                    List<Set<Point>> clusterList = analysis.subList(0, analysis.size() - 1);
                    Set<Point> noise = analysis.get(analysis.size() - 1);
                    CompletableFuture<List<ClusterStatistics>> clusterStatsListFuture =
                            futureList2ListFuture(clusterList.parallelStream().
                                    map(countClusterProvider).collect(Collectors.toList()));
                    CompletableFuture<NoiseStatistics> noiseStatsFuture = countNoiseProvider.apply(noise);
                    return clusterStatsListFuture.thenCombine(noiseStatsFuture, (csList, ns) -> {
                        csList.sort(Comparator.comparing(cs -> cs.eventsPerTick, Comparator.reverseOrder()));
                        return new WorldStatistics(csList, ns);
                    });
                });
        CompletableFuture<Map<UUID, WorldStatistics>> mapFuture =
                futureMap2MapFuture(points.entrySet().parallelStream().
                        collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> worldStatsAnalysis.apply(entry.getValue()))));
        this.future = mapFuture.thenApply(Optional::of).
                acceptEither(timeout(TIMEOUT_MINUTES, TimeUnit.MINUTES, Optional.empty()),
                        result -> {
                            this.threadPool.shutdownNow();
                            asyncCallback.accept(result);
                        });
    }

    /**
     * Return true if this process is running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return !this.future.isDone();
    }

    /**
     * Force to stop this process.
     */
    public void forceStop() {
        if (!isRunning()) return;
        this.future.cancel(false);
        this.threadPool.shutdownNow();
    }

    /**
     * Transform a list of CompletableFuture to CompletableFuture of the list.
     *
     * @param futureList List of CompletableFuture to transform
     * @param <E>        Element type
     * @return CompletableFuture of the list
     */
    private <E> CompletableFuture<List<E>> futureList2ListFuture(List<CompletableFuture<E>> futureList) {
        return CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).
                thenApply(aVoid -> futureList.stream().
                        map(CompletableFuture::join).collect(Collectors.toList()));
    }

    /**
     * Transform a map of CompletableFuture to CompletableFuture of the map.
     *
     * @param futureMap Map of CompletableFuture to transform
     * @param <K>       Key type
     * @param <V>       Value type
     * @return CompletableFuture of the map
     */
    private <K, V> CompletableFuture<Map<K, V>> futureMap2MapFuture(Map<K, CompletableFuture<V>> futureMap) {
        return CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0])).
                thenApply(aVoid -> futureMap.entrySet().parallelStream().
                        collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().join())));
    }

    /**
     * Create a CompletableFuture that just waits and then returns the specific value.
     * <br/>
     * The idiom of this function is:
     * <br/>
     * {@code [a CompletableFuture].acceptEither(timeout([timeout], [unit], [then]), [action]);}
     *
     * @param timeout Timeout time
     * @param unit    Timeout time unit
     * @param then    Value to be returned if timed out
     * @param <T>     Value type
     * @return An empty optional
     */
    private <T> CompletableFuture<T> timeout(long timeout, TimeUnit unit, T then) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(unit.toMillis(timeout));
            } catch (InterruptedException ignored) {
            }
            return then;
        });
    }

}
