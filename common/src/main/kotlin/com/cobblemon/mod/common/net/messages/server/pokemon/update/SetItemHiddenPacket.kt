/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.server.pokemon.update

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import java.util.*

/**
 * Packet sent to the server that indicates a Pokémon's held item visibility has changed.
 *
 * Handled by [SetItemHiddenHandler].
 *
 * @author joshxviii
 */
class SetItemHiddenPacket(val pokemonUUID: UUID, val isItemHidden: Boolean) :NetworkPacket<SetItemHiddenPacket> {
    override val id = ID
    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeUUID(pokemonUUID)
        buffer.writeBoolean(isItemHidden)
    }
    companion object {
        val ID: ResourceLocation = cobblemonResource("set_item_hidden")
        fun decode(buffer: RegistryFriendlyByteBuf) = SetItemHiddenPacket(
            buffer.readUUID(), buffer.readBoolean()
        )
    }
}