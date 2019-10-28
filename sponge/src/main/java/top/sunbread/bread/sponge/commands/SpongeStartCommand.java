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

    private static final int NORMAL_COLLECTION_PERIOD_MULTIPLIER = 4; // 60 seconds

    private SpongeController controller;

    public SpongeStartCommand(SpongeController controller) {
        this.controller = controller;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (this.controller.getInfo().getStatus() == SpongeController.ControllerInfo.ControllerStatus.IDLE) {
            src.sendMessage(Text.of(TextColors.YELLOW, "Sub-command ",
                    TextColors.GREEN, "start",
                    TextColors.YELLOW, " executed successfully!"));
            this.controller.startBREAD(src, NORMAL_COLLECTION_PERIOD_MULTIPLIER);
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

}
