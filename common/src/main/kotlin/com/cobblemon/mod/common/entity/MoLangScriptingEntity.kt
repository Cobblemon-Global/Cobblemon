/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity

import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.struct.VariableStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.api.ai.BrainPreset
import com.cobblemon.mod.common.api.molang.MoLangFunctions
import com.cobblemon.mod.common.api.npc.configuration.MoLangConfigVariable
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.util.DataKeys
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.resources.ResourceLocation

/**
 * An interface representing an entity that can have MoLang variables and data. Originally was part of [NPCEntity]
 * but this interface decouples that logic.
 *
 * @author Hiroku
 * @since December 7th, 2024
 */
interface MoLangScriptingEntity {
    var behavioursAreCustom: Boolean
    val behaviours: MutableList<ResourceLocation>
    val registeredVariables: MutableList<MoLangConfigVariable>
    var config: VariableStruct
    var data: VariableStruct

    fun getExtraVariables(): List<MoLangConfigVariable> = emptyList()

    fun registerVariables(brainVariables: Collection<MoLangConfigVariable>) {
        registeredVariables.clear()
        registeredVariables.addAll(brainVariables)
        registeredVariables.addAll(getExtraVariables())
    }

    fun initializeScripting() {
        registeredVariables.filter { it.variableName !in config.map }.forEach { variable -> config.setDirectly(variable.variableName, variable.type.toMoValue(variable.defaultValue)) }
        config.map.keys.filter { key -> registeredVariables.none { it.variableName == key } }.forEach { config.map.remove(it) }
    }

    fun saveScriptingToNBT(nbt: CompoundTag) {
        nbt.putBoolean(DataKeys.SCRIPTED_BRAIN_IS_CUSTOM, behavioursAreCustom)
        nbt.put(DataKeys.SCRIPTED_BRAIN_PRESETS, ListTag().also { it.addAll(behaviours.map { StringTag.valueOf(it.toString()) }) })
        nbt.put(DataKeys.SCRIPTED_DATA, MoLangFunctions.writeMoValueToNBT(data))
        nbt.put(DataKeys.SCRIPTED_CONFIG, MoLangFunctions.writeMoValueToNBT(config))
    }

    fun loadScriptingFromNBT(nbt: CompoundTag) {
        behavioursAreCustom = nbt.getBoolean(DataKeys.SCRIPTED_BRAIN_IS_CUSTOM)
        behaviours.clear()
        behaviours.addAll(nbt.getList(DataKeys.SCRIPTED_BRAIN_PRESETS, ListTag.TAG_STRING.toInt()).map { ResourceLocation.parse(it.asString) })
        data = MoLangFunctions.readMoValueFromNBT(nbt.getCompound(DataKeys.SCRIPTED_DATA)) as VariableStruct
        config = if (nbt.contains(DataKeys.SCRIPTED_CONFIG)) MoLangFunctions.readMoValueFromNBT(nbt.getCompound(DataKeys.SCRIPTED_CONFIG)) as VariableStruct else VariableStruct()
    }

    fun registerFunctionsForScripting(struct: QueryStruct) {
        struct.addFunction("config") { config }
        struct.addFunction("data") { data }
        struct.addFunction("has_variable") { params -> DoubleValue(registeredVariables.any { it.variableName == params.getString(0) }) }
    }

    fun updateBehaviours(brainPresets: Collection<ResourceLocation>)
}