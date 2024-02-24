/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.storage.player.adapter

import com.cobblemon.mod.common.api.pokedex.Pokedex
import com.cobblemon.mod.common.api.pokedex.adapter.GlobalTrackedDataAdapter
import com.cobblemon.mod.common.api.pokedex.adapter.PokedexInstanceCreator
import com.cobblemon.mod.common.api.pokedex.trackeddata.GlobalTrackedData
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreType
import com.cobblemon.mod.common.util.adapters.IdentifierAdapter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.util.Identifier
import java.util.UUID

/**
 * A [PlayerDataStoreBackend] for [PokedexPlayerData]
 *
 * @author Apion
 * @since February 22, 2024
 */
class PokedexDataJsonBackend: JsonBackedPlayerDataStoreBackend<Pokedex>("pokedex", PlayerInstancedDataStoreType.POKEDEX) {
    override val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .registerTypeAdapter(Identifier::class.java, IdentifierAdapter)
        .registerTypeAdapter(GlobalTrackedData::class.java, GlobalTrackedDataAdapter)
        .registerTypeAdapter(Pokedex::class.java, PokedexInstanceCreator)
        .create()
    override val classToken = TypeToken.get(Pokedex::class.java)
    override val defaultData = defaultDataFunc

    companion object {
        val defaultDataFunc = { uuid: UUID ->
            Pokedex(uuid)
        }
    }

}