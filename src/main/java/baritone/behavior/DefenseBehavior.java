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

package baritone.behavior;

import baritone.Baritone;
import baritone.api.behavior.IDefenseBehavior;
import baritone.api.event.events.TickEvent;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.Helper;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class DefenseBehavior extends Behavior implements IDefenseBehavior, Helper {

    private List<Entity> cache = new ArrayList<>();
    private long tickWait = 0;

    public DefenseBehavior(Baritone baritone) {
        super(baritone);
    }

    private void runBehavior() {
        if (this.tickWait > 0) {
            this.tickWait--;
            return;
        }
        if (ctx.player() != null && ctx.world() != null && mc.gameMode != null) {
            logDebug("Active Defense: " + (Baritone.settings().selfDefense.value && !cache.isEmpty()));
            logDebug("Gamemode: " + mc.gameMode);
            logDebug("Hostiles in range: " + cache);
            logDebug("Attackable hostiles: " + cache.stream().filter(this::attackable).toList());
        }
        for (Entity entity : this.cache) {
            logDebug("...on " + entity);
            if (this.attackable(entity)) {
                logDebug("Attacking " + entity);
                Optional<Rotation> reachable = RotationUtils.reachable(ctx, entity.blockPosition());
                reachable.ifPresent(r -> {
                    ctx.player().setYRot(r.getYaw());
                    ctx.player().setXRot(r.getPitch());
                });
                this.switchToSword();
                ctx.attack(entity);
            }
        }
        this.tickWait = Baritone.settings().selfDefenseAttackDelay.value;
    }

    @Override
    public void onTick(TickEvent event) {
        this.scanHostiles();
        if (Baritone.settings().selfDefense.value && !cache.isEmpty() && this.isPathing()) {
            this.runBehavior();
        }
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
        if (swordSlot.isPresent()) {
            ctx.player().getInventory().selected = swordSlot.get();
        }
    }

    private boolean attackable(Entity entity) {
        return true;
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
        if (!this.scannable()) {
            this.cache = Collections.emptyList();
            return;
        }
        this.cache = ctx.entitiesStream()
                .filter(this::validEntity)
                .filter(this::isHostileTowardsPlayer)
                .filter(this::inRange)
                .distinct()
                .collect(Collectors.toList());
    }

    private Goal towards(Entity following) {
        BlockPos pos;
        if (Baritone.settings().followOffsetDistance.value == 0) {
            pos = following.blockPosition();
        } else {
            GoalXZ g = GoalXZ.fromDirection(following.position(), Baritone.settings().followOffsetDirection.value, Baritone.settings().followOffsetDistance.value);
            pos = new BlockPos(g.getX(), following.position().y, g.getZ());
        }
        return new GoalNear(pos, Baritone.settings().followRadius.value);
    }

    private boolean scannable() {
        return ctx.world() != null && ctx.player() != null && mc.gameMode != null;
    }

    private boolean isPathing() {
        return this.baritone.getPathingControlManager().activeProcesses().size() > 0;
    }
}
