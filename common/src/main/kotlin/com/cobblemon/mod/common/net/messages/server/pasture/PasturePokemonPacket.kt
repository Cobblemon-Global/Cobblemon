/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.server.pasture

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.util.cobblemonResource
import java.util.UUID
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.math.BlockPos

/**
 * Packet sent to the server to pasture a Pokémon.
 *
 * @author Hiroku
 * @since April 9th, 2023
 */
class PasturePokemonPacket(val pokemonId: UUID, val pasturePos: BlockPos) : NetworkPacket<PasturePokemonPacket> {
    companion object {
        val ID = cobblemonResource("pasture_pokemon")

        fun decode(buffer: PacketByteBuf) = PasturePokemonPacket(buffer.readUuid(), BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt()))
    }

    override val id = ID
    override fun encode(buffer: PacketByteBuf) {
        buffer.writeUuid(pokemonId)
        buffer.writeInt(pasturePos.x)
        buffer.writeInt(pasturePos.y)
        buffer.writeInt(pasturePos.z)
    }
}