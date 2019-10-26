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
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public final class SpongeCommandSourceCheckProxy implements CommandExecutor {

    private CommandExecutor realExecutor;

    public SpongeCommandSourceCheckProxy(CommandExecutor realExecutor) {
        this.realExecutor = realExecutor;
    }

    public static SpongeCommandSourceCheckProxy of(CommandExecutor realExecuter) {
        return new SpongeCommandSourceCheckProxy(realExecuter);
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (src instanceof ConsoleSource)
            return this.realExecutor.execute(src, args);
        else if (src instanceof Player)
            return this.realExecutor.execute(src, args);
        else
            return CommandResult.empty();
    }

}
