/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.brewing;

import com.cobblemon.mod.common.item.crafting.brewingstand.BrewingStandRecipe;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandMenu.IngredientsSlot.class)
public class IngredientSlotMixin {
	@Inject(
			method = "mayPlace",
			at = @At("HEAD"),
			cancellable = true
	)
	private void cobblemon$mayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
		cir.cancel();
	}
}
