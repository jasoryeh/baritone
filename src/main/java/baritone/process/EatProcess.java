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
import baritone.api.process.IEatProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EatProcess extends BaritoneProcessHelper implements IEatProcess {
    int lastSlot = 0;
    boolean JUST_ATE = false;

    public EatProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        if (!this.inGame()) {
            return false;
        } else {
            boolean isHungry = ctx.player().getFoodData().needsFood();
            if (this.JUST_ATE && !isHungry) {
                this.JUST_ATE = false;
                mc.options.keyUse.setDown(false);
                ctx.player().getInventory().selected = lastSlot;
            }
            return Baritone.settings().autoEat.value && isHungry;
        }
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        LocalPlayer player = ctx.player();
        if (player.getFoodData().needsFood()) {
            Inventory inventory = player.getInventory();
            int foodSlot = this.findFoodSlot();
            if (foodSlot != -1) {
                this.lastSlot = player.getInventory().selected;
                player.getInventory().selected = foodSlot;
                mc.options.keyUse.setDown(true);
                this.JUST_ATE = true;
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
            //logDebug("unable to find a good food item.");
            // couldn't find a food item, so we don't pause because we don't need to stop to eat it
        }
        // not hungry! do other stuff!
        return new PathingCommand(null, PathingCommandType.DEFER);
    }

    @Override
    public void onLostControl() {
        if (this.inGame()) {
            this.JUST_ATE = false;
            mc.options.keyUse.setDown(false);
            ctx.player().getInventory().selected = lastSlot;
        }
    }

    @Override
    public String displayName0() {
        return "Eat process, using food slot: " + this.findFoodSlot();
    }

    private boolean inGame() {
        return ctx.world() != null && ctx.player() != null && mc.gameMode != null;
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
                        FoodData foodData = player.getFoodData();
                        if (player.getHealth() != player.getMaxHealth() && foodData.needsFood()) {
                            logDebug("eating because player is low on health: " + itemStack + "; nutrition: " + item.getFoodProperties().getNutrition() + "; sat: " + item.getFoodProperties().getSaturationModifier());
                            return i;
                        } else if (foodData.needsFood() && (foodData.getFoodLevel() + foodProperties.getNutrition()) <= FoodConstants.MAX_FOOD) {
                            logDebug("eating because player needs food and wont waste it: " + itemStack + "; nutrition: " + item.getFoodProperties().getNutrition() + "; sat: " + item.getFoodProperties().getSaturationModifier());
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public double priority() {
        return IEatProcess.super.priority() + 2;
    }
}
