/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.ai

import com.cobblemon.mod.common.CobblemonMemories
import com.google.common.collect.ImmutableMap
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.behavior.Behavior
import net.minecraft.world.entity.ai.memory.MemoryStatus

class StayAfloatTask(private val chance: Float = 0F) : Behavior<PathfinderMob>(
        ImmutableMap.of(CobblemonMemories.POKEMON_BATTLE, MemoryStatus.VALUE_ABSENT) // Prevents Illusion Pokemon from bouncing in the water when disguised as swimmers.
) {
    override fun checkExtraStartConditions(world: ServerLevel, entity: PathfinderMob): Boolean {
        return entity.isInWater && (entity.getFluidHeight(FluidTags.WATER) > entity.fluidJumpThreshold) || entity.isInLava
    }

    override fun canStillUse(world: ServerLevel, entity: PathfinderMob, l: Long): Boolean {
        return this.checkExtraStartConditions(world, entity)
    }

    override fun tick(world: ServerLevel, entity: PathfinderMob, l: Long) {
        if (entity.random.nextFloat() < this.chance) {
            entity.jumpControl.jump()
        }
    }
}
