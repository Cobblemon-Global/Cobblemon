/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.storage.player.client

import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreType
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.net.messages.client.SetClientPlayerDataPacket
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import java.util.UUID

/**
 * Client representation of [GeneralPlayerData]
 *
 * @author Apion
 * @since February 21, 2024
 */

data class ClientGeneralPlayerData(
    val resetStarters: Boolean? = false,
    var promptStarter: Boolean = true,
    var starterLocked: Boolean = true,
    var starterSelected: Boolean = false,
    var starterUUID: UUID? = null,
    val battleTheme: Identifier? = null
) : ClientInstancedPlayerData(false) {

    override fun encode(buf: PacketByteBuf) {
        buf.writeBoolean(promptStarter)
        buf.writeBoolean(starterLocked)
        buf.writeBoolean(starterSelected)
        val starterUUID = starterUUID
        buf.writeNullable(starterUUID) { pb, value -> pb.writeUuid(value) }
        buf.writeNullable(resetStarters) { pb, value -> pb.writeBoolean(value) }
        buf.writeIdentifier(battleTheme)
    }
    companion object {
        fun decode(buffer: PacketByteBuf): SetClientPlayerDataPacket {

            val promptStarter = buffer.readBoolean()
            val starterLocked = buffer.readBoolean()
            val starterSelected = buffer.readBoolean()
            val starterUUID = buffer.readNullable { it.readUuid() }
            val resetStarterPrompt = buffer.readNullable { it.readBoolean() }
            val battleTheme = buffer.readIdentifier()
            val data = ClientGeneralPlayerData(
                resetStarterPrompt,
                promptStarter,
                starterLocked,
                starterSelected,
                starterUUID,
                battleTheme
            )
            //Weird to do this, but since the flag doesn't get passed to the decoded obj, do it here
            //Should be fine, as long as decode doesn't get run on the server for some reason
            if (resetStarterPrompt == true) {
                CobblemonClient.checkedStarterScreen = false
                CobblemonClient.overlay.resetAttachedToast()
            }
            return SetClientPlayerDataPacket(PlayerInstancedDataStoreType.GENERAL, data)
        }

        fun runAction(data: ClientInstancedPlayerData) {
            if (data !is ClientGeneralPlayerData) return
            CobblemonClient.clientPlayerData = data
        }
    }
}