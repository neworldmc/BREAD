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

import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import top.sunbread.bread.common.BREADStatistics;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class SpigotCommand implements CommandExecutor, TabCompleter {

    private static final int NORMAL_COLLECTION_PERIOD_MULTIPLIER = 4; // 60 seconds
    private static final int SEMI_FAST_COLLECTION_PERIOD_MULTIPLIER = 2; // 30 seconds
    private static final int FAST_COLLECTION_PERIOD_MULTIPLIER = 1; // 15 seconds

    private SpigotController controller;

    SpigotCommand(SpigotController controller) {
        this.controller = controller;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("bread.admin")) {
            sender.sendMessage(ChatColor.RED + "Sorry, you don't have permission to use BREAD.");
            sender.sendMessage(ChatColor.RED +
                    "Please contact administrators if you believe that this was a mistake.");
            return true;
        }
        if (args.length > 0)
            switch (args[0].toLowerCase()) {
                case "status":
                    cmdStatus(sender);
                    break;
                case "start":
                    if (this.controller.getStatus() == SpigotController.ControllerStatus.IDLE) {
                        sender.sendMessage(ChatColor.YELLOW + "Sub-command " +
                                ChatColor.GREEN + "start" +
                                ChatColor.YELLOW + " executed successfully!");
                        this.controller.runBREAD(sender, NORMAL_COLLECTION_PERIOD_MULTIPLIER);
                    } else sender.sendMessage(ChatColor.RED + "There is already a BREAD run by " +
                            this.controller.getCurrentOperator().getName() + ".");
                    break;
                case "semi-fast":
                    if (this.controller.getStatus() == SpigotController.ControllerStatus.IDLE) {
                        sender.sendMessage(ChatColor.YELLOW + "Sub-command " +
                                ChatColor.GREEN + "semi-fast" +
                                ChatColor.YELLOW + " executed successfully!");
                        this.controller.runBREAD(sender, SEMI_FAST_COLLECTION_PERIOD_MULTIPLIER);
                    } else sender.sendMessage(ChatColor.RED + "There is already a BREAD run by " +
                            this.controller.getCurrentOperator().getName() + ".");
                    break;
                case "fast":
                    if (this.controller.getStatus() == SpigotController.ControllerStatus.IDLE) {
                        sender.sendMessage(ChatColor.YELLOW + "Sub-command " +
                                ChatColor.GREEN + "fast" +
                                ChatColor.YELLOW + " executed successfully!");
                        this.controller.runBREAD(sender, FAST_COLLECTION_PERIOD_MULTIPLIER);
                    } else sender.sendMessage(ChatColor.RED + "There is already a BREAD run by " +
                            this.controller.getCurrentOperator().getName() + ".");
                    break;
                case "stop":
                    if (this.controller.getStatus() != SpigotController.ControllerStatus.IDLE) {
                        sender.sendMessage(ChatColor.YELLOW + "Sub-command " +
                                ChatColor.GREEN + "stop" +
                                ChatColor.YELLOW + " executed successfully!");
                        this.controller.stopBREAD(sender);
                    } else sender.sendMessage(ChatColor.RED + "There is no running BREAD.");
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "There is no such sub-command.");
                    break;
            }
        else
            sender.sendMessage(ChatColor.RED + "Please specify sub-command which you want to perform.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (sender.hasPermission("bread.admin") && args.length == 1)
            return Stream.of("status", "start", "fast", "stop").
                    filter(subCmd -> subCmd.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).
                    collect(Collectors.toList());
        else return Collections.emptyList();
    }

    private void cmdStatus(CommandSender sender) {
        switch (this.controller.getStatus()) {
            case IDLE:
                sender.sendMessage(ChatColor.YELLOW + "BREAD Status: " + ChatColor.GREEN + "Idle");
                if (this.controller.getLastResult() != null) {
                    if (this.controller.getLastResult().size() == 0)
                        sender.sendMessage(ChatColor.YELLOW + "Last Result is existing but empty");
                    else {
                        sender.sendMessage(ChatColor.YELLOW + "======== Last Result ========");
                        for (Map.Entry<UUID, BREADStatistics.WorldStatistics> entry :
                                this.controller.getLastResult().entrySet()) {
                            String worldName = Bukkit.getWorld(entry.getKey()).getName();
                            sender.sendMessage(ChatColor.GOLD + "---- " + worldName + " ----");
                            BREADStatistics.WorldStatistics stats = entry.getValue();
                            List<BREADStatistics.ClusterStatistics> clusters = stats.clusters;
                            for (BREADStatistics.ClusterStatistics cluster : clusters)
                                sender.spigot().sendMessage(getRowComponents(cluster));
                            if (stats.noise.raw.size() != 0)
                                sender.spigot().sendMessage(getRowComponents(stats.noise));
                        }
                    }
                } else
                    sender.sendMessage(ChatColor.YELLOW + "Last Result does not exist");
                break;
            case COLLECTING:
                sender.sendMessage(ChatColor.YELLOW + "BREAD Status: " + ChatColor.AQUA + "Collecting");
                sender.sendMessage(ChatColor.YELLOW + "Operator: " +
                        ChatColor.DARK_GREEN + this.controller.getCurrentOperator().getName());
                break;
            case ANALYSING:
                sender.sendMessage(ChatColor.YELLOW + "BREAD Status: " + ChatColor.AQUA + "Analysing");
                sender.sendMessage(ChatColor.YELLOW + "Operator: " +
                        ChatColor.DARK_GREEN + this.controller.getCurrentOperator().getName());
                break;
        }
    }

    private BaseComponent[] getRowComponents(BREADStatistics.ClusterStatistics stats) {
        return mergeComponents(getStatsTypeComponents(false),
                getCommaComponents(),
                getValueItemComponents("EPT", "Events per tick",
                        stats.eventsPerTick, this::frequencyRound),
                getCommaComponents(),
                getLocationItemComponents("ECL", "Event centroid location",
                        stats.eventCentroidLocation, this::locationAndDistanceRound),
                getCommaComponents(),
                getValueItemComponents("DFC", "Distance from centroid",
                        stats.distanceFromCentroid, this::locationAndDistanceRound),
                getCommaComponents(),
                getDataItemComponents("PDC", "Points data code\n" +
                                "Format: [X list, Y list, Z list, Event times list]\n" +
                                "Encoding scheme: Base64",
                        raw2MATLABString(stats.raw).getBytes(StandardCharsets.UTF_8)));
    }

    private BaseComponent[] getRowComponents(BREADStatistics.NoiseStatistics stats) {
        return mergeComponents(getStatsTypeComponents(true),
                getCommaComponents(),
                getValueItemComponents("EPT", "Events per tick",
                        stats.eventsPerTick, this::frequencyRound),
                getCommaComponents(),
                getDataItemComponents("PDC", "Points data code\n" +
                                "Format: [X list, Y list, Z list, Event times list]\n" +
                                "Encoding scheme: Base64",
                        raw2MATLABString(stats.raw).getBytes(StandardCharsets.UTF_8)));
    }

    private BaseComponent[] mergeComponents(BaseComponent[]... parts) {
        return Arrays.stream(parts).
                map(Arrays::stream).
                reduce(Arrays.stream(new BaseComponent[0]), Stream::concat).
                toArray(BaseComponent[]::new);
    }

    private BaseComponent[] getCommaComponents() {
        TextComponent comma = new TextComponent(", ");
        comma.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        return new BaseComponent[]{comma};
    }

    private BaseComponent[] getStatsTypeComponents(boolean noise) {
        TextComponent statsType;
        if (noise) {
            statsType = new TextComponent("Not region");
            statsType.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        } else {
            statsType = new TextComponent("Region");
            statsType.setColor(net.md_5.bungee.api.ChatColor.AQUA);
        }
        return new BaseComponent[]{statsType};
    }

    private BaseComponent[] getValueItemComponents(String name, String nameMeaning,
                                                   double value, DoubleFunction<String> round) {
        TextComponent valueComponent = new TextComponent(round.apply(value));
        valueComponent.setColor(net.md_5.bungee.api.ChatColor.BLUE);
        return new BaseComponent[]{getItemNameComponent(name, nameMeaning), getEqualsSignComponent(),
                valueComponent};
    }

    private BaseComponent[] getLocationItemComponents(String name, String nameMeaning,
                                                      double[] location, DoubleFunction<String> round) {
        TextComponent locationComponent = new TextComponent(location2String(location, round));
        locationComponent.setColor(net.md_5.bungee.api.ChatColor.BLUE);
        locationComponent.setUnderlined(true);
        locationComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to teleport here").
                        color(net.md_5.bungee.api.ChatColor.YELLOW).create()));
        locationComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/tp " + round.apply(location[0]) +
                        " " + round.apply(location[1]) +
                        " " + round.apply(location[2])));
        return new BaseComponent[]{getItemNameComponent(name, nameMeaning), getEqualsSignComponent(),
                locationComponent};
    }

    private BaseComponent[] getDataItemComponents(String name, String nameMeaning, byte[] data) {
        String base64Data = Base64.getEncoder().encodeToString(data);
        String[] dataParts = splitStringByLength(base64Data, 256);
        List<BaseComponent> dataComponentList = new ArrayList<>();
        dataComponentList.addAll(Arrays.asList(new ComponentBuilder("[").
                color(net.md_5.bungee.api.ChatColor.BLUE).create()));
        for (int i = 0; i < dataParts.length; ++i) {
            TextComponent dataComponent = new TextComponent(String.valueOf(i % 10));
            if (i % 2 == 0) dataComponent.setColor(net.md_5.bungee.api.ChatColor.RED);
            else dataComponent.setColor(net.md_5.bungee.api.ChatColor.LIGHT_PURPLE);
            dataComponent.setUnderlined(true);
            dataComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Click to get this part of data").
                            color(i % 2 == 0 ?
                                    net.md_5.bungee.api.ChatColor.RED : net.md_5.bungee.api.ChatColor.LIGHT_PURPLE).
                            create()));
            dataComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, dataParts[i]));
            dataComponentList.add(dataComponent);
        }
        dataComponentList.addAll(Arrays.asList(new ComponentBuilder("]").
                color(net.md_5.bungee.api.ChatColor.BLUE).create()));
        return mergeComponents(new BaseComponent[]{getItemNameComponent(name, nameMeaning), getEqualsSignComponent()},
                dataComponentList.toArray(new BaseComponent[0]));
    }

    private String raw2MATLABString(Set<BREADStatistics.Point> points) {
        int[][] matrix = points.stream().
                map(point -> new int[]{point.x, point.y, point.z, point.w}).
                toArray(int[][]::new);
        return IntStream.range(0, 4).
                mapToObj(i -> Arrays.stream(matrix).
                        map(point -> point[i]).
                        map(String::valueOf).
                        collect(Collectors.joining(", ", "[", "]"))).
                collect(Collectors.joining(", ", "[", "]"));
    }

    private String frequencyRound(double a) {
        DecimalFormat formatter = new DecimalFormat("0.##");
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(a);
    }

    private String locationAndDistanceRound(double a) {
        DecimalFormat formatter = new DecimalFormat("0.00");
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(a);
    }

    private BaseComponent getItemNameComponent(String name, String nameMeaning) {
        TextComponent nameComponent = new TextComponent(name);
        nameComponent.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);
        if (nameMeaning != null)
            nameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(nameMeaning).
                            color(net.md_5.bungee.api.ChatColor.AQUA).create()));
        return nameComponent;
    }

    private BaseComponent getEqualsSignComponent() {
        TextComponent equalsSign = new TextComponent("=");
        equalsSign.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        return equalsSign;
    }

    private String location2String(double[] location, DoubleFunction<String> round) {
        return Arrays.stream(location).boxed().
                map(round::apply).collect(Collectors.joining(", ", "(", ")"));
    }

    private String[] splitStringByLength(String string, int charLength) {
        List<String> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder(string);
        while (buffer.length() != 0) {
            result.add(buffer.substring(0, Math.min(charLength, buffer.length())));
            buffer.delete(0, charLength);
        }
        return result.toArray(new String[0]);
    }

}
