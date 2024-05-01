/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.storage.player.adapter

import com.cobblemon.mod.common.api.pokedex.PokedexRecord
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreType

class PokedexDataNbtBackend : NbtBackedPlayerData<PokedexRecord>("pokedex", PlayerInstancedDataStoreType.POKEDEX) {
    override val codec = PokedexRecord.CODEC
    override val defaultData = PokedexDataJsonBackend.defaultDataFunc

}