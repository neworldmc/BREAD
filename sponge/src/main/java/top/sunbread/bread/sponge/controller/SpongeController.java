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

import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import top.sunbread.bread.common.BREADAnalyser;
import top.sunbread.bread.common.BREADStatistics;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SpongeController {

    private Game game;
    private PluginContainer plugin;
    private ControllerInfo info;
    private SpongeCollector collector;
    private BREADAnalyser analyser;

    public SpongeController(Game game, PluginContainer plugin) {
        this.game = game;
        this.plugin = plugin;
        this.info = new ControllerInfo(this.game);
        this.collector = null;
        this.analyser = null;
    }

    public ControllerInfo getInfo() {
        return this.info;
    }

    public void startBREAD(CommandSource source, int collectionPeriodMultiplier) {
        if (this.info.getStatus() != ControllerInfo.ControllerStatus.IDLE || source == null)
            return;
        collectingStage(source, collectionPeriodMultiplier);
    }

    public void stopBREAD(CommandSource source) {
        if (this.info.getStatus() == ControllerInfo.ControllerStatus.IDLE)
            return;
        if (this.collector != null) {
            this.collector.forceStop();
            this.collector = null;
        }
        if (this.analyser != null) {
            this.analyser.forceStop();
            this.analyser = null;
        }
        this.info.setStatus(ControllerInfo.ControllerStatus.IDLE);
        if (source == null)
            notifyOperator(Text.of(TextColors.YELLOW, "BREAD is stopped by server."));
        else
            notifyOperator(Text.of(TextColors.YELLOW, "BREAD is stopped by ",
                    TextColors.DARK_GREEN, source.getName(),
                    TextColors.YELLOW, "."));
        this.info.setCurrentOperator(null);
    }

    public Optional<String> getWorldName(UUID worldUID) {
        if (!this.game.getServer().getWorld(worldUID).isPresent()) return Optional.empty();
        return Optional.of(this.game.getServer().getWorld(worldUID).get().getName());
    }

    private void collectingStage(CommandSource source, int collectionPeriodMultiplier) {
        this.info.setStatus(ControllerInfo.ControllerStatus.COLLECTING);
        this.info.setCurrentOperator(source);
        this.info.setLastResult(null);
        notifyOperator(Text.of(TextColors.YELLOW, "BREAD is collecting redstone events..."));
        notifyOperator(Text.of(TextColors.YELLOW, "This process will take " +
                BREADAnalyser.COLLECTING_TICKS_BASE * collectionPeriodMultiplier + " game-ticks (" +
                BREADAnalyser.COLLECTING_TICKS_BASE / 20 * collectionPeriodMultiplier + " seconds)."));
        this.collector = new SpongeCollector(this.game, this.plugin,
                BREADAnalyser.COLLECTING_TICKS_BASE * collectionPeriodMultiplier,
                points -> analysingStage(points, collectionPeriodMultiplier));
    }

    private void analysingStage(Map<UUID, Set<BREADStatistics.Point>> points, int collectionPeriodMultiplier) {
        this.collector = null;
        this.info.setStatus(ControllerInfo.ControllerStatus.ANALYSING);
        notifyOperator(Text.of(TextColors.YELLOW, "BREAD is analysing the data collected in the previous step..."));
        notifyOperator(Text.of(TextColors.YELLOW, "This process will take a while. Sit back and relax."));
        this.analyser = new BREADAnalyser(points, collectionPeriodMultiplier,
                statistics -> Task.builder().execute(() -> {
                    if (statistics.isPresent())
                        finalStage(statistics.get());
                    else
                        finalStageFailure();
                }).submit(this.plugin.getInstance().get()));
    }

    private void finalStage(Map<UUID, BREADStatistics.WorldStatistics> statistics) {
        this.analyser = null;
        this.info.setStatus(ControllerInfo.ControllerStatus.IDLE);
        this.info.setLastResult(statistics);
        notifyOperator(Text.of(TextColors.YELLOW, "BREAD is completed!"));
        notifyOperator(Text.of(TextColors.YELLOW, "Use sub-command ", TextColors.GREEN, "status",
                TextColors.YELLOW, " to view the diagnosis."));
        this.info.setCurrentOperator(null);
    }

    private void finalStageFailure() {
        this.analyser = null;
        this.info.setStatus(ControllerInfo.ControllerStatus.IDLE);
        this.info.setLastResult(null);
        notifyOperator(Text.of(TextColors.YELLOW, "BREAD failed! Timed out analysing."));
    }

    private void notifyOperator(Text message) {
        final Text prefix = Text.of(TextColors.GOLD, "[", TextColors.YELLOW, "BREAD", TextColors.GOLD, "]",
                TextColors.RESET, " ");
        this.info.sendMessageToCurrentOperator(Text.builder().append(prefix).append(message).build());
    }

    public static final class ControllerInfo {

        private Game game;
        private ControllerStatus status;
        private String currentOperatorName;
        private OperatorType operatorType;
        private UUID playerOperatorUUID;
        private Map<UUID, BREADStatistics.WorldStatistics> lastResult;

        ControllerInfo(Game game) {
            this.game = game;
            this.status = ControllerStatus.IDLE;
            this.currentOperatorName = null;
            this.operatorType = null;
            this.playerOperatorUUID = null;
            this.lastResult = null;
        }

        public ControllerStatus getStatus() {
            return this.status;
        }

        public Optional<String> getCurrentOperatorName() {
            return Optional.ofNullable(this.currentOperatorName);
        }

        public void sendMessageToCurrentOperator(Text message) {
            if (this.operatorType == null) return;
            Optional<CommandSource> source = Optional.empty();
            switch (this.operatorType) {
                case CONSOLE:
                    source = this.game.getServer().getConsole().getCommandSource();
                    break;
                case PLAYER:
                    source = upcastOptional(this.game.getServer().getPlayer(this.playerOperatorUUID));
                    break;
            }
            source.ifPresent(present -> present.sendMessage(message));
        }

        public Optional<Map<UUID, BREADStatistics.WorldStatistics>> getLastResult() {
            return Optional.ofNullable(this.lastResult);
        }

        void setStatus(ControllerStatus status) {
            if (status == null) throw new NullPointerException();
            this.status = status;
        }

        void setCurrentOperator(CommandSource source) {
            if (source == null) {
                this.currentOperatorName = null;
                this.operatorType = null;
                this.playerOperatorUUID = null;
            }
            if (source instanceof ConsoleSource) {
                this.currentOperatorName = source.getName();
                this.operatorType = OperatorType.CONSOLE;
                this.playerOperatorUUID = null;
            }
            if (source instanceof Player) {
                this.currentOperatorName = source.getName();
                this.operatorType = OperatorType.PLAYER;
                this.playerOperatorUUID = ((Player) source).getUniqueId();
            }
        }

        void setLastResult(Map<UUID, BREADStatistics.WorldStatistics> lastResult) {
            this.lastResult = lastResult;
        }

        @SuppressWarnings("all")
        private <T> Optional<T> upcastOptional(Optional<? extends T> optional) {
            return (Optional<T>) optional;
        }

        public enum ControllerStatus {IDLE, COLLECTING, ANALYSING}

        private enum OperatorType {CONSOLE, PLAYER}

    }

}
