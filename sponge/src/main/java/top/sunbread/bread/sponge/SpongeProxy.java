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

package top.sunbread.bread.sponge;

import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import top.sunbread.bread.BREAD;
import top.sunbread.bread.sponge.commands.SpongeCommandSourceCheckProxy;
import top.sunbread.bread.sponge.commands.SpongeStartCommand;
import top.sunbread.bread.sponge.commands.SpongeStatusCommand;
import top.sunbread.bread.sponge.commands.SpongeStopCommand;
import top.sunbread.bread.sponge.controller.SpongeController;

public final class SpongeProxy {

    private final Game game;
    private final PluginContainer plugin;
    private SpongeController controller;
    private boolean enabled;

    public SpongeProxy(Game game, PluginContainer plugin) {
        this.game = game;
        this.plugin = plugin;
        this.enabled = false;
    }

    public void onGameInitialization() {
        this.plugin.getLogger().info("Copyright (C) " + BREAD.COPYRIGHT_DATE + " Sunbread.");
        this.plugin.getLogger().info("");
        this.plugin.getLogger().info("This program is free software: you can redistribute it and/or modify");
        this.plugin.getLogger().info("it under the terms of the GNU Affero General Public License as published by");
        this.plugin.getLogger().info("the Free Software Foundation, either version 3 of the License, or");
        this.plugin.getLogger().info("(at your option) any later version.");
        this.plugin.getLogger().info("");
        this.plugin.getLogger().info("This program is distributed in the hope that it will be useful,");
        this.plugin.getLogger().info("but WITHOUT ANY WARRANTY; without even the implied warranty of");
        this.plugin.getLogger().info("MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
        this.plugin.getLogger().info("GNU Affero General Public License for more details.");
        this.plugin.getLogger().info("");
        this.plugin.getLogger().info("You should have received a copy of the GNU Affero General Public License");
        this.plugin.getLogger().info("along with this program.  If not, see <https://www.gnu.org/licenses/>.");
        if (Integer.parseInt(System.getProperty("java.version").split("_")[0].split("\\.")[0]) != 1 ||
                Integer.parseInt(System.getProperty("java.version").split("_")[0].split("\\.")[1]) != 8) {
            this.plugin.getLogger().error("This plugin only works on Java 8!");
            return;
        }
        if (this.game.getPlatform().getExecutionType() != Platform.Type.SERVER) {
            this.plugin.getLogger().error("This plugin only works on server platform!");
            return;
        }
        if (!this.plugin.getInstance().isPresent()) {
            this.plugin.getLogger().error("Cannot get instance of this plugin!");
            return;
        }
        this.controller = new SpongeController(this.game, this.plugin);
        CommandSpec statusCommand = CommandSpec.builder().
                description(Text.of("To view status of BREAD")).
                executor(SpongeCommandSourceCheckProxy.of(new SpongeStatusCommand(this.controller))).
                build();
        CommandSpec startCommand = CommandSpec.builder().
                description(Text.of("To start BREAD")).
                executor(SpongeCommandSourceCheckProxy.of(new SpongeStartCommand(this.controller, false))).
                build();
        CommandSpec startFastCommand = CommandSpec.builder().
                description(Text.of("To start fast BREAD")).
                executor(SpongeCommandSourceCheckProxy.of(new SpongeStartCommand(this.controller, true))).
                build();
        CommandSpec stopCommand = CommandSpec.builder().
                description(Text.of("To stop running BREAD")).
                executor(SpongeCommandSourceCheckProxy.of(new SpongeStopCommand(this.controller))).
                build();
        CommandSpec baseCommand = CommandSpec.builder().
                permission("bread.admin").
                description(Text.of("Base command for BREAD")).
                child(statusCommand, "status").
                child(startCommand, "start").
                child(startFastCommand, "fast").
                child(stopCommand, "stop").
                build();
        this.game.getCommandManager().register(this.plugin.getInstance().get(), baseCommand, "bread");
        this.enabled = true;
        this.plugin.getLogger().info("Enabled");
    }

    public void onGameStopping() {
        if (!this.enabled) return;
        this.enabled = false;
        this.controller.stopBREAD(null);
        this.plugin.getLogger().info("Disabled");
    }

}
