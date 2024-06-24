/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client

import com.cobblemon.mod.common.BakingOverride
import com.cobblemon.mod.common.util.cobblemonModel
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.util.Identifier

/**
 * The purpose of this class is to hold models that we want baked, but aren't associated with
 * any block state. Actual registration happens in ModelLoaderMixin/ModelLoader
 */
object CobblemonBakingOverrides {
    val models = mutableListOf<BakingOverride>()

    // Blocks
    val RESTORATION_TANK_FLUID_BUBBLING = registerOverride(
        cobblemonResource("block/restoration_tank_fluid_bubbling"),
        cobblemonModel("restoration_tank_fluid_bubbling", "none")
    )
    val RESTORATION_TANK_FLUID_CHUNKED_1 = registerOverride(
        cobblemonResource("block/restoration_tank_fluid_chunked_1"),
        cobblemonModel("restoration_tank_fluid_chunked", "1")
    )
    val RESTORATION_TANK_FLUID_CHUNKED_2 = registerOverride(
        cobblemonResource("block/restoration_tank_fluid_chunked_2"),
        cobblemonModel("restoration_tank_fluid_chunked", "2")
    )
    val RESTORATION_TANK_FLUID_CHUNKED_3 = registerOverride(
        cobblemonResource("block/restoration_tank_fluid_chunked_3"),
        cobblemonModel("restoration_tank_fluid_chunked", "3")
    )
    val RESTORATION_TANK_FLUID_CHUNKED_4 = registerOverride(
        cobblemonResource("block/restoration_tank_fluid_chunked_4"),
        cobblemonModel("restoration_tank_fluid_chunked", "4")
    )
    val RESTORATION_TANK_FLUID_CHUNKED_5 = registerOverride(
        cobblemonResource("block/restoration_tank_fluid_chunked_5"),
        cobblemonModel("restoration_tank_fluid_chunked", "5")
    )
    val RESTORATION_TANK_FLUID_CHUNKED_6 = registerOverride(
        cobblemonResource("block/restoration_tank_fluid_chunked_6"),
        cobblemonModel("restoration_tank_fluid_chunked", "6")
    )
    val RESTORATION_TANK_FLUID_CHUNKED_7 = registerOverride(
        cobblemonResource("block/restoration_tank_fluid_chunked_7"),
        cobblemonModel("restoration_tank_fluid_chunked", "7")
    )
    val RESTORATION_TANK_FLUID_CHUNKED_8 = registerOverride(
        cobblemonResource("block/restoration_tank_fluid_chunked_8"),
        cobblemonModel("restoration_tank_fluid_chunked", "8")
    )
    val RESTORATION_TANK_CONNECTOR = registerOverride(
        cobblemonResource("block/restoration_tank_connector"),
        cobblemonModel("restoration_tank_connector", "none")
    )

    // Items
    val POKEDEX_BLACK = registerOverride(
        cobblemonResource("item/pokedex_black_model"),
        cobblemonModel("pokedex_black_model", "inventory")
    )
    val POKEDEX_BLUE = registerOverride(
        cobblemonResource("item/pokedex_blue_model"),
        cobblemonModel("pokedex_blue_model", "inventory")
    )
    val POKEDEX_GREEN = registerOverride(
        cobblemonResource("item/pokedex_green_model"),
        cobblemonModel("pokedex_green_model", "inventory")
    )
    val POKEDEX_PINK = registerOverride(
        cobblemonResource("item/pokedex_pink_model"),
        cobblemonModel("pokedex_pink_model", "inventory")
    )
    val POKEDEX_RED = registerOverride(
        cobblemonResource("item/pokedex_red_model"),
        cobblemonModel("pokedex_red_model", "inventory")
    )
    val POKEDEX_WHITE = registerOverride(
        cobblemonResource("item/pokedex_white_model"),
        cobblemonModel("pokedex_white_model", "inventory")
    )
    val POKEDEX_YELLOW = registerOverride(
        cobblemonResource("item/pokedex_yellow_model"),
        cobblemonModel("pokedex_yellow_model", "inventory")
    )

    fun getPokedexOverride(type: String): BakingOverride {
        when (type) {
            "black" -> return POKEDEX_BLACK
            "blue" -> return POKEDEX_BLUE
            "green" -> return POKEDEX_GREEN
            "pink" -> return POKEDEX_PINK
            "white" -> return POKEDEX_WHITE
            "yellow" -> return POKEDEX_YELLOW
        }
        return POKEDEX_RED
    }

    fun registerOverride(modelLocation: Identifier, modelIdentifier: ModelIdentifier): BakingOverride {
        val result = BakingOverride(modelLocation, modelIdentifier)
        models.add(result)
        return result
    }
}