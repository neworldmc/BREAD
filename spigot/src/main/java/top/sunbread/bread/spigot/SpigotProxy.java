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
import org.bukkit.plugin.java.JavaPlugin;
import top.sunbread.bread.BREAD;

public final class SpigotProxy {

    private final JavaPlugin plugin;
    private boolean fail;
    private SpigotController controller;

    public SpigotProxy(JavaPlugin plugin) {
        this.plugin = plugin;
        this.fail = true;
    }

    public void onEnable() {
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
        this.fail = false;
        if ((Integer.parseInt(System.getProperty("java.version").split("_")[0].split("\\.")[0]) <= 1 &&
                Integer.parseInt(System.getProperty("java.version").split("_")[0].split("\\.")[1]) < 8) ||
                Integer.parseInt(System.getProperty("java.version").split("_")[0].split("\\.")[0]) > 11) {
            this.plugin.getLogger().severe("This plugin only works on Java 8 to 11!");
            this.fail = true;
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            return;
        }
        if (Package.getPackage("org.spigotmc") == null) {
            this.plugin.getLogger().severe("This plugin only works on Spigot (or its derivatives)!");
            this.fail = true;
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            return;
        }
        if (Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[0]) <= 1 &&
                Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]) < 13) {
            this.plugin.getLogger().severe("This plugin only works on Minecraft server 1.13 (or higher versions)!");
            this.fail = true;
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            return;
        }
        this.fail = false;
        this.controller = new SpigotController(this.plugin);
        SpigotCommand command = new SpigotCommand(this.controller);
        this.plugin.getCommand("bread").setExecutor(command);
        this.plugin.getCommand("bread").setTabCompleter(command);
        this.plugin.getLogger().info("Enabled");
    }

    public void onDisable() {
        if (this.fail) return;
        this.plugin.getCommand("bread").setExecutor(null);
        this.plugin.getCommand("bread").setTabCompleter(null);
        this.controller.stopBREAD(null);
        this.controller = null;
        this.plugin.getLogger().info("Disabled");
    }

}
