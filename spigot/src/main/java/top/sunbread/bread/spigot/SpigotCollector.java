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

package top.sunbread.bread.spigot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.java.JavaPlugin;
import top.sunbread.bread.common.BREADStatistics;

import java.util.*;
import java.util.stream.Collectors;

final class SpigotCollector {

    private Map<UUID, Map<HashPoint, Integer>> points; // World UID, Hash Point, Repeat Times
    private JavaPlugin plugin;
    private boolean running;
    private Listener listener;

    SpigotCollector(JavaPlugin plugin) {
        this.points = new HashMap<>();
        this.plugin = plugin;
        this.running = false;
        this.listener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
                addPointFromLocation(event.getBlock().getLocation());
            }
        };
    }

    void start() {
        if (this.running) return;
        Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
        this.running = true;
    }

    void stop() {
        if (!this.running) return;
        this.running = false;
        HandlerList.unregisterAll(this.listener);
    }

    boolean isRunning() {
        return this.running;
    }

    Set<World> getWorlds() {
        if (this.running) return null;
        return this.points.keySet().parallelStream().map(Bukkit::getWorld).collect(Collectors.toSet());
    }

    Set<BREADStatistics.Point> getPointsForWorld(World world) {
        if (this.running) return null;
        return this.points.get(world.getUID()).entrySet().parallelStream().
                map(entry -> new BREADStatistics.Point(entry.getKey().x, entry.getKey().y, entry.getKey().z,
                        entry.getValue())).
                collect(Collectors.toSet());
    }

    Map<UUID, Set<BREADStatistics.Point>> getPoints() {
        if (this.running) return null;
        return getWorlds().parallelStream().collect(Collectors.toMap(World::getUID, this::getPointsForWorld));
    }

    void clear() {
        if (this.running) return;
        this.points.clear();
    }

    private void addPointFromLocation(Location loc) {
        Map<HashPoint, Integer> pointsInWorld;
        if (!this.points.containsKey(loc.getWorld().getUID()))
            this.points.put(loc.getWorld().getUID(), new LinkedHashMap<>());
        pointsInWorld = this.points.get(loc.getWorld().getUID());
        HashPoint point = new HashPoint(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        pointsInWorld.put(point, pointsInWorld.containsKey(point) ? pointsInWorld.get(point) + 1 : 1);
    }

    private static final class HashPoint {

        int x, y, z;

        HashPoint(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (this == o) return true;
            if (!(o instanceof HashPoint)) return false;
            return this.x == ((HashPoint) o).x && this.y == ((HashPoint) o).y && this.z == ((HashPoint) o).z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.x, this.y, this.z);
        }

    }

}
