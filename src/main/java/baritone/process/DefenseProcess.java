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

package baritone.process;

import baritone.Baritone;
import baritone.api.process.IDefenseProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

public class DefenseProcess extends BaritoneProcessHelper implements IDefenseProcess {
    private Set<Entity> cache = new HashSet<>();
    private long tickWait = 0;

    public DefenseProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        this.scanHostiles();
        return this.inGame() && Baritone.settings().selfDefense.value && !cache.isEmpty();
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (this.tickWait > 0) {
            this.tickWait--;
            // well if we are waiting on combat cooldown, it should be safe to switch items
            if (this.cache.size() > 0) {
                // defer if no more hostiles nearby, otherwise keep attacking
                return new PathingCommand(null, PathingCommandType.DEFER);
            } else {
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        }
        LocalPlayer player = ctx.player();
        Level world = ctx.world();
        if (player != null && world != null && mc.gameMode != null) {
            logDebug("Hostiles in range: " + cache);
        }
        for (Entity entity : this.cache) {
            logDebug("Attacking " + entity);
            Optional<Rotation> reachable = RotationUtils.reachable(ctx, entity.blockPosition());
            reachable.ifPresent(r -> {
                player.setYRot(r.getYaw());
                player.setXRot(r.getPitch());
            });
            this.switchToSword();
            ctx.attack(entity);
            this.tickWait = Baritone.settings().selfDefenseAttackDelay.value;
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
        return new PathingCommand(null, PathingCommandType.DEFER);
    }

    @Override
    public void onLostControl() {
        this.cache = new HashSet<>();
        this.tickWait = 0;
    }

    @Override
    public String displayName0() {
        return "Defending against: " + this.cache;
    }

    public Optional<Integer> getSwordSlot() {
        Inventory inventory = ctx.player().getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (!(itemStack.getItem() instanceof SwordItem)) {
                continue;
            }

            if (Baritone.settings().itemSaver.value && (itemStack.getDamageValue() + Baritone.settings().itemSaverThreshold.value) >= itemStack.getMaxDamage() && itemStack.getMaxDamage() > 1) {
                continue;
            }

            return Optional.of(i);
        }
        return Optional.empty();
    }

    public void switchToSword() {
        Optional<Integer> swordSlot = this.getSwordSlot();
        swordSlot.ifPresent(integer -> ctx.player().getInventory().selected = integer);
    }

    private boolean validEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        if (entity == ctx.player()) {
            return false;
        }
        return true;
    }

    private boolean isHostileTowardsPlayer(Entity entity) {
        return entity instanceof Enemy;
    }

    private boolean inRange(Entity entity) {
        LocalPlayer player = ctx.player();
        if (player == null) {
            return false;
        }
        double distanceTo = player.position().distanceTo(entity.position());
        return distanceTo <= Baritone.settings().selfDefenseRadius.value;
    }

    private void scanHostiles() {
        if (this.inGame()) {
            this.cache = ctx.entitiesStream()
                    .filter(this::validEntity)
                    .filter(this::isHostileTowardsPlayer)
                    .filter(this::inRange)
                    .collect(Collectors.toSet());
        } else {
            this.cache = Collections.emptySet();
        }
    }

    private boolean inGame() {
        return ctx.world() != null && ctx.player() != null && mc.gameMode != null;
    }

    @Override
    public double priority() {
        return IDefenseProcess.super.priority() + 1;
    }
}
