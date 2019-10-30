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
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import top.sunbread.bread.common.BREADStatistics;

import java.util.*;
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
                if (transaction.isValid() && transaction.getDefault().getLocation().isPresent() &&
                        isTraitValueDifferent(transaction.getOriginal().getState(),
                                transaction.getFinal().getState()))
                    addPoint(transaction.getDefault().getLocation().get());
        }

        private boolean isTraitValueDifferent(BlockState blockState1, BlockState blockState2) {
            return isPoweredTraitValueDifferent(blockState1, blockState2) ||
                    isPowerTraitValueDifferent(blockState1, blockState2);
        }

        private boolean isPoweredTraitValueDifferent(BlockState blockState1, BlockState blockState2) {
            Optional<BlockTrait<Boolean>> poweredTrait1 = getPoweredTrait(blockState1);
            Optional<BlockTrait<Boolean>> poweredTrait2 = getPoweredTrait(blockState2);
            if (!poweredTrait1.isPresent() || !poweredTrait2.isPresent()) return false;
            Optional<Boolean> poweredTraitValue1 = blockState1.getTraitValue(poweredTrait1.get());
            Optional<Boolean> poweredTraitValue2 = blockState2.getTraitValue(poweredTrait2.get());
            if (!poweredTraitValue1.isPresent() || !poweredTraitValue2.isPresent()) return false;
            return !poweredTraitValue1.get().equals(poweredTraitValue2.get());
        }

        private Optional<BlockTrait<Boolean>> getPoweredTrait(BlockState blockState) {
            Optional<BlockTrait<?>> optionalTrait = blockState.getType().getTrait("powered");
            if (!optionalTrait.isPresent()) return Optional.empty();
            BlockTrait<?> genericTrait = optionalTrait.get();
            if (!genericTrait.getValueClass().equals(Boolean.class)) return Optional.empty();
            @SuppressWarnings("unchecked")
            BlockTrait<Boolean> trait = (BlockTrait<Boolean>) genericTrait;
            return Optional.of(trait);
        }

        private boolean isPowerTraitValueDifferent(BlockState blockState1, BlockState blockState2) {
            Optional<BlockTrait<Integer>> powerTrait1 = getPowerTrait(blockState1);
            Optional<BlockTrait<Integer>> powerTrait2 = getPowerTrait(blockState2);
            if (!powerTrait1.isPresent() || !powerTrait2.isPresent()) return false;
            Optional<Integer> powerTraitValue1 = blockState1.getTraitValue(powerTrait1.get());
            Optional<Integer> powerTraitValue2 = blockState2.getTraitValue(powerTrait2.get());
            if (!powerTraitValue1.isPresent() || !powerTraitValue2.isPresent()) return false;
            return !powerTraitValue1.get().equals(powerTraitValue2.get());
        }

        private Optional<BlockTrait<Integer>> getPowerTrait(BlockState blockState) {
            Optional<BlockTrait<?>> optionalTrait = blockState.getType().getTrait("power");
            if (!optionalTrait.isPresent()) return Optional.empty();
            BlockTrait<?> genericTrait = optionalTrait.get();
            if (!genericTrait.getValueClass().equals(Integer.class)) return Optional.empty();
            @SuppressWarnings("unchecked")
            BlockTrait<Integer> trait = (BlockTrait<Integer>) genericTrait;
            return Optional.of(trait);
        }

    }

}
