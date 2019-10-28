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
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import top.sunbread.bread.common.BREADAnalyser;
import top.sunbread.bread.common.BREADStatistics;

import java.util.Map;
import java.util.UUID;

final class SpigotController {

    private JavaPlugin plugin;
    private ControllerStatus status;
    private Operator currentOperator;
    private Map<UUID, BREADStatistics.WorldStatistics> lastResult;
    private SpigotCollectorScheduler scheduler;
    private BREADAnalyser analyser;

    SpigotController(JavaPlugin plugin) {
        this.plugin = plugin;
        this.status = ControllerStatus.IDLE;
        this.currentOperator = null;
        this.scheduler = null;
        this.analyser = null;
        this.lastResult = null;
    }

    ControllerStatus getStatus() {
        return this.status;
    }

    Operator getCurrentOperator() {
        return this.currentOperator;
    }

    Map<UUID, BREADStatistics.WorldStatistics> getLastResult() {
        return this.lastResult;
    }

    void runBREAD(CommandSender sender, int collectionPeriodMultiplier) {
        if (this.status != ControllerStatus.IDLE || sender == null) return;
        this.currentOperator = new Operator(sender);
        this.status = ControllerStatus.COLLECTING;
        this.lastResult = null;
        notifyOperator("BREAD is collecting redstone events...");
        notifyOperator("This process will take " +
                BREADAnalyser.COLLECTING_TICKS_BASE * collectionPeriodMultiplier + " game-ticks (" +
                BREADAnalyser.COLLECTING_TICKS_BASE / 20 * collectionPeriodMultiplier + " seconds).");
        this.scheduler = new SpigotCollectorScheduler(this.plugin, points -> {
            this.status = ControllerStatus.ANALYSING;
            notifyOperator("BREAD is analysing the data collected in the previous step...");
            notifyOperator("This process will take a while. Sit back and relax.");
            this.scheduler = null;
            this.analyser = new BREADAnalyser(points, collectionPeriodMultiplier,
                    result -> Bukkit.getScheduler().runTask(this.plugin, () -> {
                        this.status = ControllerStatus.IDLE;
                        if (result.isPresent()) {
                            this.lastResult = result.get();
                            notifyOperator("BREAD is completed!");
                            notifyOperator("Use sub-command " + ChatColor.GREEN +
                                    "status" + ChatColor.RESET +
                                    " to view the diagnosis.");
                        } else {
                            this.lastResult = null;
                            notifyOperator("BREAD failed! Timed out analysing.");
                        }
                        this.analyser = null;
                        this.currentOperator = null;
                    }));
        }, BREADAnalyser.COLLECTING_TICKS_BASE * collectionPeriodMultiplier);
    }

    void stopBREAD(CommandSender sender) {
        if (this.status == ControllerStatus.IDLE) return;
        if (this.scheduler != null) {
            this.scheduler.forceStop();
            this.scheduler = null;
        }
        if (this.analyser != null) {
            this.analyser.forceStop();
            this.analyser = null;
        }
        if (sender == null)
            notifyOperator("BREAD is stopped by server.");
        else
            notifyOperator("BREAD is stopped by " + ChatColor.DARK_GREEN + sender.getName() + ChatColor.RESET + ".");
        this.status = ControllerStatus.IDLE;
        this.currentOperator = null;
    }

    private void notifyOperator(String message) {
        final String prefix = ChatColor.GOLD + "[" +
                ChatColor.YELLOW + "BREAD" +
                ChatColor.GOLD + "]" +
                ChatColor.RESET +
                " " + ChatColor.YELLOW;
        this.currentOperator.sendMessage(prefix +
                message.replace(String.valueOf(ChatColor.RESET), String.valueOf(ChatColor.YELLOW)));
    }

    enum ControllerStatus {IDLE, COLLECTING, ANALYSING}

    static final class Operator {

        private String name;
        private boolean flagEntity;
        private UUID entityUUID;
        private CommandSender others;

        Operator(CommandSender sender) {
            if (sender == null) throw new NullPointerException();
            this.name = sender.getName();
            if (sender instanceof Entity) {
                this.flagEntity = true;
                this.entityUUID = ((Entity) sender).getUniqueId();
            } else {
                this.flagEntity = false;
                this.others = sender;
            }
        }

        String getName() {
            return this.name;
        }

        void sendMessage(String message) {
            CommandSender target;
            if (this.flagEntity) {
                target = Bukkit.getEntity(this.entityUUID);
                if (target == null) return;
            } else target = this.others;
            target.sendMessage(message);
        }

    }

}
