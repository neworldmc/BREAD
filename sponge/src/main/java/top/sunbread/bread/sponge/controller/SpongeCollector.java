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

package top.sunbread.bread.sponge.controller;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutablePoweredData;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableRedstonePoweredData;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import top.sunbread.bread.common.BREADStatistics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class SpongeCollector {

    private Game game;
    private PluginContainer plugin;
    private Map<UUID, Map<Vector3i, Integer>> points; // World UID, Hash Point, Repeat Times
    private BlockRedstoneEventListener listener;
    private Task task;
    private boolean running;

    SpongeCollector(Game game, PluginContainer plugin, int collectionPeriod,
                    Consumer<Map<UUID, Set<BREADStatistics.Point>>> callback) {
        this.game = game;
        this.plugin = plugin;
        this.points = new HashMap<>();
        this.listener = new BlockRedstoneEventListener();
        this.game.getEventManager().registerListeners(this.plugin.getInstance().get(), this.listener);
        this.task = Task.builder().execute(() -> {
            this.running = false;
            this.game.getEventManager().unregisterListeners(this.listener);
            callback.accept(formatPoints(this.points));
            this.points.clear();
        }).delayTicks(collectionPeriod).submit(this.plugin.getInstance().get());
        this.running = true;
    }

    boolean isRunning() {
        return this.running;
    }

    void forceStop() {
        if (!isRunning()) return;
        this.running = false;
        this.task.cancel();
        this.game.getEventManager().unregisterListeners(this.listener);
        this.points.clear();
    }

    private void addPoint(Location<World> location) {
        UUID worldUID = location.getExtent().getUniqueId();
        if (!this.points.containsKey(worldUID)) this.points.put(worldUID, new HashMap<>());
        Map<Vector3i, Integer> worldPoints = this.points.get(worldUID);
        Vector3i pointPos = location.getBlockPosition();
        worldPoints.put(pointPos, worldPoints.containsKey(pointPos) ? worldPoints.get(pointPos) + 1 : 1);
    }

    private static Map<UUID, Set<BREADStatistics.Point>> formatPoints(Map<UUID, Map<Vector3i, Integer>> source) {
        return source.entrySet().parallelStream().unordered().
                collect(Collectors.toMap(Map.Entry::getKey,
                        world -> world.getValue().entrySet().parallelStream().unordered().
                                map(duplicatePoints -> new BREADStatistics.Point(
                                        duplicatePoints.getKey().getX(),
                                        duplicatePoints.getKey().getY(),
                                        duplicatePoints.getKey().getZ(),
                                        duplicatePoints.getValue()
                                )).
                                collect(Collectors.toSet())));
    }

    public final class BlockRedstoneEventListener {

        @Listener(order = Order.LAST)
        public void onBlockChange(ChangeBlockEvent.Modify event) {
            for (Transaction<BlockSnapshot> transaction : event.getTransactions())
                if (transaction.isValid()) {
                    transaction.getDefault().get(ImmutablePoweredData.class).ifPresent(ignored -> {
                        boolean originalPowered =
                                transaction.getOriginal().get(ImmutablePoweredData.class).get().
                                        powered().get();
                        boolean finalPowered =
                                transaction.getFinal().get(ImmutablePoweredData.class).get().
                                        powered().get();
                        if (originalPowered != finalPowered)
                            transaction.getDefault().getLocation().ifPresent(SpongeCollector.this::addPoint);
                    });
                    transaction.getDefault().get(ImmutableRedstonePoweredData.class).ifPresent(ignored -> {
                        int originalPower =
                                transaction.getOriginal().get(ImmutableRedstonePoweredData.class).get().
                                        power().get();
                        int finalPower =
                                transaction.getFinal().get(ImmutableRedstonePoweredData.class).get().
                                        power().get();
                        if (originalPower != finalPower)
                            transaction.getDefault().getLocation().ifPresent(SpongeCollector.this::addPoint);
                    });
                }
        }

    }

}
