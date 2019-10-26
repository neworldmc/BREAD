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

package top.sunbread.bread;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import top.sunbread.bread.spigot.SpigotProxy;

public final class BREAD extends JavaPlugin {

    public static final String COPYRIGHT_DATE = "${project.build.currentYear}";

    private SpigotProxy spigotProxy;

    @Override
    public void onLoad() {
        this.spigotProxy = new SpigotProxy(this);
    }

    @Override
    public void onEnable() {
        new Metrics(this);
        this.spigotProxy.onEnable();
    }

    @Override
    public void onDisable() {
        this.spigotProxy.onDisable();
    }

}
