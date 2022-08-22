/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.cache.WorldScanner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class RoutineCommand extends Command {

    public RoutineCommand(IBaritone baritone) {
        super(baritone, "ores", "logs");
    }

    public static final Block[] ORES = {
            Blocks.COAL_ORE,
            Blocks.COPPER_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.GOLD_ORE,
            Blocks.IRON_ORE,
            Blocks.LAPIS_ORE,
            Blocks.REDSTONE_ORE,
            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.NETHER_GOLD_ORE,
            Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS
    };

    public static final Block[] LOGS = {
            Blocks.OAK_LOG,
            Blocks.SPRUCE_LOG,
            Blocks.BIRCH_LOG,
            Blocks.JUNGLE_LOG,
            Blocks.ACACIA_LOG,
            Blocks.DARK_OAK_LOG,
            Blocks.MANGROVE_LOG,
            Blocks.STRIPPED_SPRUCE_LOG,
            Blocks.STRIPPED_BIRCH_LOG,
            Blocks.STRIPPED_JUNGLE_LOG,
            Blocks.STRIPPED_ACACIA_LOG,
            Blocks.STRIPPED_DARK_OAK_LOG,
            Blocks.STRIPPED_OAK_LOG,
            Blocks.STRIPPED_MANGROVE_LOG
    };

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (label.equalsIgnoreCase("ores")) {
            this.mineCollection(ORES);
        } else if(label.equalsIgnoreCase("logs")) {
            this.mineCollection(LOGS);
        } else {
            logDirect("unknown routine");
        }
    }

    public void mineCollection(Block... bs) {
        WorldScanner.INSTANCE.repack(ctx);
        logDirect(String.format("Mining %s", Arrays.stream(bs).map(Block::toString).toList()));
        baritone.getMineProcess().mine(0, ORES);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Mine stuff";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Mines stuff.",
                "",
                "Usage:",
                "> ores",
                "> logs"
        );
    }
}
