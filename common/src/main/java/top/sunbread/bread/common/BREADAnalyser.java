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

public final class BREADAnalyser {

    public static final int FAST_COLLECTING_TICKS = 300;
    public static final int COLLECTING_TICKS = 1200;
    public static final int THREADS = 16;
    public static final int TIMEOUT_MINUTES = 5;

    private volatile ExecutorService threadPool;
    private CompletableFuture<Void> future;

    public BREADAnalyser(Map<UUID, Set<Point>> points, boolean fast,
                         Consumer<Optional<Map<UUID, WorldStatistics>>> asyncCallback) {
        this.threadPool = Executors.newFixedThreadPool(THREADS);
        Function<Set<Point>, CompletableFuture<List<Set<Point>>>> clusterAnalysisProvider =
                p -> CompletableFuture.supplyAsync(() -> BREADAnalysis.clusterAnalysis(p, fast), this.threadPool);
        Function<Set<Point>, CompletableFuture<ClusterStatistics>> countClusterProvider =
                c -> CompletableFuture.supplyAsync(() -> BREADAnalysis.countCluster(c, fast), this.threadPool);
        Function<Set<Point>, CompletableFuture<NoiseStatistics>> countNoiseProvider =
                n -> CompletableFuture.supplyAsync(() -> BREADAnalysis.countNoise(n, fast), this.threadPool);
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
        this.future = mapFuture.thenApply(Optional::of).acceptEither(timeout(TIMEOUT_MINUTES, TimeUnit.MINUTES),
                result -> {
                    this.threadPool.shutdownNow();
            asyncCallback.accept(result);
        });
    }

    public boolean isRunning() {
        return !this.future.isDone();
    }

    public void forceStop() {
        if (!isRunning()) return;
        this.future.cancel(false);
        this.threadPool.shutdownNow();
    }

    private <E> CompletableFuture<List<E>> futureList2ListFuture(List<CompletableFuture<E>> futureList) {
        return CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).
                thenApply(aVoid -> futureList.stream().
                        map(CompletableFuture::join).collect(Collectors.toList()));
    }

    private <K, V> CompletableFuture<Map<K, V>> futureMap2MapFuture(Map<K, CompletableFuture<V>> futureMap) {
        return CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0])).
                thenApply(aVoid -> futureMap.entrySet().parallelStream().
                        collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().join())));
    }

    private <T> CompletableFuture<Optional<T>> timeout(long timeout, TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(unit.toMillis(timeout));
            } catch (InterruptedException ignored) {
            }
            return Optional.empty();
        });
    }

}
