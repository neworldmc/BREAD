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

import com.google.inject.Inject;
import org.bstats.sponge.Metrics2;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import top.sunbread.bread.sponge.SpongeProxy;

@Plugin(id = "${project.sponge.modID}",
        name = "${project.name}",
        version = "${project.version}",
        description = "${project.description}",
        authors = {"Sunbread"})
public final class BREAD {

    public static final String COPYRIGHT_DATE = "${project.build.currentYear}";

    private SpongeProxy spongeProxy;
    @Inject
    private Game game;
    @Inject
    private PluginContainer plugin;
    @Inject
    private Metrics2 metrics;

    @Listener
    public void onGameConstruction(GameConstructionEvent event) {
        this.spongeProxy = new SpongeProxy(this.game, this.plugin);
    }

    @Listener
    public void onGameInitialization(GameInitializationEvent event) {
        this.spongeProxy.onGameInitialization();
    }

    @Listener
    public void onGameStopping(GameStoppingEvent event) {
        this.spongeProxy.onGameStopping();
    }

}
