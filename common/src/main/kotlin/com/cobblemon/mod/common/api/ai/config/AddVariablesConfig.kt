/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.ai.config

import com.cobblemon.mod.common.api.ai.BrainConfigurationContext
import com.cobblemon.mod.common.api.npc.configuration.MoLangConfigVariable
import com.cobblemon.mod.common.entity.MoLangScriptingEntity
import net.minecraft.world.entity.LivingEntity

class AddVariablesConfig : BrainConfig {
    override fun getVariables(entity: LivingEntity) = mutableListOf<MoLangConfigVariable>()

    // Configuration happens naturally through the override on variables
    override fun configure(entity: LivingEntity, brainConfigurationContext: BrainConfigurationContext) {}
}