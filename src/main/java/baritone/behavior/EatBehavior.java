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
import baritone.api.behavior.IEatBehavior;
import baritone.api.event.events.TickEvent;
import baritone.api.utils.Helper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EatBehavior extends Behavior implements IEatBehavior, Helper {

    boolean JUST_ATE = false;

    public EatBehavior(Baritone baritone) {
        super(baritone);
    }

    private void executeBehavior() {
        LocalPlayer player = ctx.player();
        if (player.getFoodData().needsFood()) {
            Inventory inventory = player.getInventory();
            int foodSlot = this.findFoodSlot();
            if (foodSlot == -1) {
                logDebug("unable to find a good food item.");
                return;
            }
            ItemStack foodStack = inventory.getItem(foodSlot);
            logDebug("eating " + foodStack);
            player.getInventory().selected = foodSlot;
            mc.options.keyUse.setDown(true);
            this.JUST_ATE = true;
        } else if (this.JUST_ATE) {
            this.JUST_ATE = false;
            mc.options.keyUse.setDown(false);
        }
    }

    private int findFoodSlot() {
        LocalPlayer player = ctx.player();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack != null) {
                Item item = itemStack.getItem();
                if (item != null) {
                    FoodProperties foodProperties = item.getFoodProperties();
                    if (foodProperties != null) {
                        logDebug("found food: " + itemStack);
                        FoodData foodData = player.getFoodData();
                        if (player.getHealth() != player.getMaxHealth() && foodData.needsFood()) {
                            logDebug("eating because player is low on health: " + itemStack);
                            return i;
                        } else if (foodData.needsFood() && (foodData.getFoodLevel() + foodProperties.getNutrition()) <= FoodConstants.MAX_FOOD) {
                            logDebug("eating because player needs food and an optimal food exists: " + itemStack);
                            return i;
                        } else {
                            logDebug("not eating: " + itemStack);
                        }
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public void onTick(TickEvent event) {
        if (Baritone.settings().autoEat.value && this.inGame() && this.isPathing()) {
            this.executeBehavior();
        }
    }

    private boolean inGame() {
        return ctx.world() != null && ctx.player() != null && mc.gameMode != null;
    }

    private boolean isPathing() {
        return this.baritone.getPathingControlManager().activeProcesses().size() > 0;
    }
}
