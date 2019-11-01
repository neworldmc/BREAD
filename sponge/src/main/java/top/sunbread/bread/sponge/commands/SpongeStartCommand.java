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

package top.sunbread.bread.sponge.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import top.sunbread.bread.sponge.controller.SpongeController;

public final class SpongeStartCommand implements CommandExecutor {

    private SpongeController controller;
    private CollectingMode mode;

    public SpongeStartCommand(SpongeController controller, CollectingMode mode) {
        this.controller = controller;
        this.mode = mode;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (this.controller.getInfo().getStatus() == SpongeController.ControllerInfo.ControllerStatus.IDLE) {
            src.sendMessage(Text.of(TextColors.YELLOW, "Sub-command ",
                    TextColors.GREEN, this.mode.getCommandName(),
                    TextColors.YELLOW, " executed successfully!"));
            this.controller.startBREAD(src, this.mode.getCollectionPeriodMultiplier());
            return CommandResult.success();
        } else {
            if (this.controller.getInfo().getCurrentOperatorName().isPresent())
                src.sendMessage(Text.of(TextColors.RED, "There is already a BREAD run by " +
                        this.controller.getInfo().getCurrentOperatorName().get() + "."));
            else
                src.sendMessage(Text.of(TextColors.RED, "There is already a running BREAD."));
            return CommandResult.empty();
        }
    }

    public enum CollectingMode {

        FAST("fast", 1), // 15 seconds
        SEMI_FAST("semi-fast", 2), // 30 seconds
        NORMAL("start", 4); // 60 seconds

        private String commandName;
        private int collectionPeriodMultiplier;

        CollectingMode(String commandName, int collectionPeriodMultiplier) {
            this.commandName = commandName;
            this.collectionPeriodMultiplier = collectionPeriodMultiplier;
        }

        String getCommandName() {
            return commandName;
        }

        int getCollectionPeriodMultiplier() {
            return collectionPeriodMultiplier;
        }

    }

}
