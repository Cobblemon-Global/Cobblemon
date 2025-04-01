package com.cobblemon.mod.common.net.messages.server.pokemon.update

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourState
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.RegistryFriendlyByteBuf

class ServerboundUpdateRidingStatePacket(val entity: Int, val state: RidingBehaviourState? = null, val data: RegistryFriendlyByteBuf? = null) : NetworkPacket<ServerboundUpdateRidingStatePacket> {
    override val id = ID

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        if (state == null) error("Expected state to be populated for encoding")
        buffer.writeInt(entity)
        state.encode(buffer)
    }

    companion object {
        val ID = cobblemonResource("c2s_update_ride_controller")
        fun decode(buffer: RegistryFriendlyByteBuf) = ServerboundUpdateRidingStatePacket(
            entity = buffer.readInt(),
            data = buffer
        )
    }
}
