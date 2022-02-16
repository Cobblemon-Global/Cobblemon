package com.cablemc.pokemoncobbled.forge.common.entity.pokemon

import com.cablemc.pokemoncobbled.forge.common.api.entity.EntitySideDelegate

/** Handles purely server logic for a Pokémon */
class PokemonServerDelegate : EntitySideDelegate<PokemonEntity> {
    override fun initialize(entity: PokemonEntity) {
        with(entity) {
            speed = 0.35F
            registerGoals()
        }
    }

    override fun tick(entity: PokemonEntity) {
        val isMoving = entity.deltaMovement.length() > 0.1
        if (isMoving && !entity.isMoving.get()) {
            entity.isMoving.set(true)
        } else if (!isMoving && entity.isMoving.get()) {
            entity.isMoving.set(false)
        }
    }
}