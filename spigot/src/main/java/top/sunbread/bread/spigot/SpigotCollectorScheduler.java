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

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import top.sunbread.bread.common.BREADStatistics;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

final class SpigotCollectorScheduler {

    private SpigotCollector collector;
    private BukkitTask task;

    SpigotCollectorScheduler(JavaPlugin plugin, Consumer<Map<UUID, Set<BREADStatistics.Point>>> callback,
                             int collectionPeriod) {
        this.collector = new SpigotCollector(plugin);
        this.collector.start();
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                SpigotCollectorScheduler.this.collector.stop();
                callback.accept(SpigotCollectorScheduler.this.collector.getPoints());
                SpigotCollectorScheduler.this.collector.clear();
            }
        }.runTaskLater(plugin, collectionPeriod);
    }

    boolean isRunning() {
        return this.collector.isRunning();
    }

    void forceStop() {
        if (!isRunning()) return;
        this.task.cancel();
        this.collector.stop();
        this.collector.clear();
    }

}
