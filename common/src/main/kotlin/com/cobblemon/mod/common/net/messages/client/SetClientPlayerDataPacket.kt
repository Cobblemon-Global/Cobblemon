/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.api.storage.player.InstancedPlayerData
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreType
import com.cobblemon.mod.common.api.storage.player.client.ClientInstancedPlayerData
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.PacketByteBuf

/**
 * Packet to update some [InstancedPlayerData] on the client
 *
 * @author Hiroku, Apion
 * @since August 1st, 2022
 */
class SetClientPlayerDataPacket(val type: PlayerInstancedDataStoreType, val playerData: ClientInstancedPlayerData) : NetworkPacket<SetClientPlayerDataPacket> {

    override val id = ID

    override fun encode(buffer: PacketByteBuf) {
        buffer.writeEnumConstant(type)
        playerData.encode(buffer)
    }

    companion object {
        val ID = cobblemonResource("set_client_playerdata")
        fun decode(buffer: PacketByteBuf): SetClientPlayerDataPacket {
            val type = buffer.readEnumConstant(PlayerInstancedDataStoreType::class.java)
            return type.decoder.invoke(buffer)
        }
    }
}