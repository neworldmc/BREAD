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
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import top.sunbread.bread.common.BREADStatistics;
import top.sunbread.bread.sponge.controller.SpongeController;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SpongeStatusCommand implements CommandExecutor {

    private SpongeController controller;

    public SpongeStatusCommand(SpongeController controller) {
        this.controller = controller;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (this.controller.getInfo().getStatus() == SpongeController.ControllerInfo.ControllerStatus.IDLE)
            src.sendMessage(Text.of(TextColors.YELLOW, "BREAD Status: ",
                    TextColors.GREEN, "Idle"));
        if (this.controller.getInfo().getStatus() == SpongeController.ControllerInfo.ControllerStatus.COLLECTING)
            src.sendMessage(Text.of(TextColors.YELLOW, "BREAD Status: ",
                    TextColors.AQUA, "Collecting"));
        if (this.controller.getInfo().getStatus() == SpongeController.ControllerInfo.ControllerStatus.ANALYSING)
            src.sendMessage(Text.of(TextColors.YELLOW, "BREAD Status: ",
                    TextColors.AQUA, "Analysing"));
        if (this.controller.getInfo().getCurrentOperatorName().isPresent())
            src.sendMessage(Text.of(TextColors.YELLOW, "Operator: ",
                    TextColors.DARK_GREEN, this.controller.getInfo().getCurrentOperatorName().get()));
        if (this.controller.getInfo().getLastResult().isPresent()) {
            Map<UUID, BREADStatistics.WorldStatistics> lastResult = this.controller.getInfo().getLastResult().get();
            if (lastResult.size() == 0)
                src.sendMessage(Text.of(TextColors.YELLOW, "Last Result is existing but empty"));
            else PaginationList.builder().
                    title(Text.builder("Last Result").color(TextColors.YELLOW).build()).
                    contents(formatLastResult(lastResult)).
                    padding(Text.builder("=").color(TextColors.GOLD).build()).
                    sendTo(src);
        }
        return CommandResult.success();
    }

    private List<Text> formatLastResult(Map<UUID, BREADStatistics.WorldStatistics> lastResult) {
        List<Text> formatted = new ArrayList<>();
        for (Map.Entry<UUID, BREADStatistics.WorldStatistics> worldEntry : lastResult.entrySet()) {
            Optional<String> optionalWorldName = this.controller.getWorldName(worldEntry.getKey());
            if (!optionalWorldName.isPresent()) continue;
            String worldName = optionalWorldName.get();
            for (BREADStatistics.ClusterStatistics cluster : worldEntry.getValue().clusters)
                formatted.add(formatStatistics(worldName, cluster));
            if (!worldEntry.getValue().noise.raw.isEmpty())
                formatted.add(formatStatistics(worldName, worldEntry.getValue().noise));
        }
        return formatted;
    }

    private Text formatStatistics(String worldName, BREADStatistics.ClusterStatistics statistics) {
        return Text.of(TextColors.GREEN, worldName,
                comma(),
                TextColors.AQUA, "Region",
                comma(),
                getItemNameText("EPT", "Events per tick"),
                equalsSign(),
                getItemValueText(statistics.eventsPerTick, this::frequencyRound),
                comma(),
                getItemNameText("ECL", "Event centroid location"),
                equalsSign(),
                getItemValueText(statistics.eventCentroidLocation, this::locationAndDistanceRound),
                comma(),
                getItemNameText("DFC", "Distance from centroid"),
                equalsSign(),
                getItemValueText(statistics.distanceFromCentroid, this::locationAndDistanceRound),
                comma(),
                getItemNameText("PDC", "Points data code\n" +
                        "Format: [X list, Y list, Z list, Event times list]\n" +
                        "Encoding scheme: Base64"),
                equalsSign(),
                getItemValueText(raw2MATLABString(statistics.raw).getBytes(StandardCharsets.UTF_8))
        );
    }

    private Text formatStatistics(String worldName, BREADStatistics.NoiseStatistics statistics) {
        return Text.of(TextColors.GREEN, worldName,
                comma(),
                TextColors.GRAY, "Non-region",
                comma(),
                getItemNameText("EPT", "Events per tick"),
                equalsSign(),
                getItemValueText(statistics.eventsPerTick, this::frequencyRound),
                comma(),
                getItemNameText("PDC", "Points data code\n" +
                        "Format: [X list, Y list, Z list, Event times list]\n" +
                        "Encoding scheme: Base64"),
                equalsSign(),
                getItemValueText(raw2MATLABString(statistics.raw).getBytes(StandardCharsets.UTF_8))
        );
    }

    private Text comma() {
        return Text.of(TextColors.GRAY, ", ");
    }

    private Text equalsSign() {
        return Text.of(TextColors.GRAY, "=");
    }

    private Text getItemNameText(String name, String meaning) {
        if (meaning != null)
            return Text.builder(name).
                    color(TextColors.DARK_AQUA).
                    onHover(TextActions.showText(Text.builder(meaning).color(TextColors.AQUA).build())).
                    build();
        else
            return Text.builder(name).color(TextColors.DARK_AQUA).build();
    }

    private Text getItemValueText(double value, DoubleFunction<String> round) {
        return Text.builder(round.apply(value)).color(TextColors.BLUE).build();
    }

    private Text getItemValueText(double[] location, DoubleFunction<String> round) {
        return Text.builder(location2String(location, round)).
                color(TextColors.BLUE).
                style(TextStyles.UNDERLINE).
                onHover(TextActions.showText(Text.builder("Click to get location").
                        color(TextColors.YELLOW).
                        build())).
                onClick(TextActions.suggestCommand("/tp " +
                        round.apply(location[0]) + " " +
                        round.apply(location[1]) + " " +
                        round.apply(location[2]))).
                build();
    }

    private Text getItemValueText(byte[] data) {
        final int partLength = 256;
        String[] dataParts = splitStringByLength(Base64.getEncoder().encodeToString(data), partLength);
        List<Text> dataPartTexts = new LinkedList<>();
        for (int i = 0; i < dataParts.length; ++i)
            dataPartTexts.add(Text.builder(String.valueOf(i % 10)).
                    color(i % 2 == 0 ? TextColors.RED : TextColors.LIGHT_PURPLE).
                    style(TextStyles.UNDERLINE).
                    onHover(TextActions.showText(Text.builder("Click to get this part of data").
                            color(i % 2 == 0 ? TextColors.RED : TextColors.LIGHT_PURPLE).
                            build())).
                    onClick(TextActions.suggestCommand(dataParts[i])).
                    build());
        return Text.of(Text.builder("[").color(TextColors.BLUE).build(),
                Text.join(dataPartTexts),
                Text.builder("]").color(TextColors.BLUE).build());
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

    private String location2String(double[] location, DoubleFunction<String> round) {
        return "(" +
                round.apply(location[0]) +
                ", " +
                round.apply(location[1]) +
                ", " +
                round.apply(location[2]) +
                ")";
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

}
