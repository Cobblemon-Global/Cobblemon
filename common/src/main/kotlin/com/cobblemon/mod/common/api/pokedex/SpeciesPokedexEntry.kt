/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.pokedex

import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import com.cobblemon.mod.common.api.events.pokemon.TradeCompletedEvent
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent
import com.cobblemon.mod.common.api.events.starter.StarterChosenEvent
import com.cobblemon.mod.common.api.pokedex.trackeddata.SpeciesTrackedData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.PrimitiveCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.Identifier
import java.util.Optional
import java.util.UUID
import kotlin.jvm.optionals.getOrDefault

/**
 * Information about a species in the dex
 *
 * @author Apion
 * @since February 24, 2024
 */
class SpeciesPokedexEntry(
    var formEntries: MutableMap<String, FormPokedexEntry> = mutableMapOf(),
    val speciesStats: MutableSet<SpeciesTrackedData> = mutableSetOf()
) {



    fun pokemonCaught(event: PokemonCapturedEvent) {
        val formStr = event.pokemon.form.formOnlyShowdownId()
        if (!formEntries.containsKey(formStr)) {
            formEntries[formStr] = FormPokedexEntry()
        }
        formEntries[formStr]?.knowledge = PokedexProgress.CAUGHT
    }

    fun pokemonEvolved(event: EvolutionCompleteEvent) {
        val formStr = event.pokemon.form.formOnlyShowdownId()
        if (!formEntries.containsKey(formStr)) {
            formEntries[formStr] = FormPokedexEntry()
        }
        formEntries[formStr]?.knowledge = PokedexProgress.CAUGHT
    }

    fun pokemonTraded(event: TradeCompletedEvent, ownerUuid: UUID) {
        val recievedPokemon = if (event.tradeParticipant1.uuid == ownerUuid) event.tradeParticipant2Pokemon else event.tradeParticipant1Pokemon
        val formStr = recievedPokemon.form.formOnlyShowdownId()
        if (!formEntries.containsKey(formStr)) {
            formEntries[formStr] = FormPokedexEntry()
        }
        formEntries[formStr]?.knowledge = PokedexProgress.CAUGHT
    }

    fun pokemonSeen(speciesId: Identifier, formStr: String) {
        if (!formEntries.containsKey(formStr)) {
            formEntries[formStr] = FormPokedexEntry()
        }
        val knowledge = formEntries[formStr]?.knowledge
        if (knowledge != PokedexProgress.CAUGHT) {
            formEntries[formStr]?.knowledge = PokedexProgress.ENCOUNTERED
        }

    }

    fun starterChosen(event: StarterChosenEvent) {
        val formStr = event.pokemon.form.formOnlyShowdownId()
        if (!formEntries.containsKey(formStr)) {
            formEntries[formStr] = FormPokedexEntry()
        }
        formEntries[formStr]?.knowledge = PokedexProgress.CAUGHT
    }

    companion object {
        val CODEC: Codec<SpeciesPokedexEntry> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.unboundedMap(PrimitiveCodec.STRING, FormPokedexEntry.CODEC).fieldOf("formEntries").forGetter { it.formEntries },
                Codec.list(SpeciesTrackedData.CODEC).optionalFieldOf("speciesStats").forGetter {
                    if (it.speciesStats.isEmpty()) {
                        return@forGetter Optional.empty()
                    }
                    return@forGetter Optional.of(it.speciesStats.toList())
                }
            ).apply(instance) { formEntries, speciesStats ->
                return@apply SpeciesPokedexEntry(formEntries.toMutableMap(), speciesStats.getOrDefault(mutableListOf()).toMutableSet())
            }

        }
    }
}