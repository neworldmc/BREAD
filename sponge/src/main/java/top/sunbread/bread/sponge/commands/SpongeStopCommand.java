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

public final class SpongeStopCommand implements CommandExecutor {

    private SpongeController controller;

    public SpongeStopCommand(SpongeController controller) {
        this.controller = controller;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (this.controller.getInfo().getStatus() != SpongeController.ControllerInfo.ControllerStatus.IDLE) {
            src.sendMessage(Text.of(TextColors.YELLOW, "Sub-command ",
                    TextColors.GREEN, "stop",
                    TextColors.YELLOW, " executed successfully!"));
            this.controller.stopBREAD(src);
            return CommandResult.success();
        } else {
            src.sendMessage(Text.of(TextColors.RED, "There is no running BREAD."));
            return CommandResult.empty();
        }
    }

}
