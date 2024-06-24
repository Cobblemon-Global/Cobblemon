/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex.widgets

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokedex.SpeciesPokedexEntry
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.HALF_OVERLAY_HEIGHT
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.HALF_OVERLAY_WIDTH
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.POKEMON_DESCRIPTION_HEIGHT
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.POKEMON_PORTRAIT_HEIGHT
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.POKEMON_PORTRAIT_WIDTH
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.PORTRAIT_POKE_BALL_HEIGHT
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.PORTRAIT_POKE_BALL_WIDTH
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCALE
import com.cobblemon.mod.common.client.gui.summary.widgets.SoundlessWidget
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.drawScaledTextJustifiedRight
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonFloatingState
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.pokedex.DexPokemonData
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.RenderablePokemon
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import org.joml.Quaternionf
import org.joml.Vector3f

class SizeWidget(val pX: Int, val pY: Int) : SoundlessWidget(
    pX,
    pY,
    HALF_OVERLAY_WIDTH,
    POKEMON_DESCRIPTION_HEIGHT,
    lang("ui.pokedex.pokemon_info")
) {
    companion object {
        private val scrollBorder = cobblemonResource("textures/gui/pokedex/info_scroll_border.png")
        private val heightGrid = cobblemonResource("textures/gui/pokedex/height_grid.png")
        private val gridPlayer = cobblemonResource("textures/gui/pokedex/height_grid_player.png")
    }

    var renderablePokemon : RenderablePokemon? = null
    var baseScale: Float = 1F
    var height: Float = 0F
    var weight: Float = 0F

    override fun renderButton(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val matrices = context.matrices

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = Text.translatable("cobblemon.ui.pokedex.height", (height / 10).toString()).bold(),
            x = pX + 9,
            y = pY - 10,
            shadow = true
        )

        drawScaledTextJustifiedRight(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = Text.translatable("cobblemon.ui.pokedex.weight", (weight / 10).toString()).bold(),
            x = pX + HALF_OVERLAY_WIDTH - 9,
            y = pY - 10,
            shadow = true
        )

        context.enableScissor(
            pX + 1,
            pY,
            pX + HALF_OVERLAY_WIDTH - 1,
            pY + POKEMON_DESCRIPTION_HEIGHT
        )

        matrices.push()
        matrices.translate(
            pX.toDouble() + 50,
            pY.toDouble() + (POKEMON_DESCRIPTION_HEIGHT / 2) + 10,
            0.0
        )
        drawProfilePokemon(
            renderablePokemon = renderablePokemon!!,
            matrixStack =  matrices,
            partialTicks = delta,
            rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(0F, 0F, 0F)),
            state = null,
            scale = 7.5F,
            applyProfileTransform = false,
            applyBaseScale = true,
            r = 0F,
            g = 0F,
            b = 0F
        )
        matrices.pop()
        context.disableScissor()

        // Ensure elements are not hidden behind Pokémon render
        matrices.push()
        matrices.translate(0.0, 0.0, 100.0)

        blitk(
            matrixStack = context.matrices,
            texture = gridPlayer,
            x = pX + 85,
            y = pY + 23.5,
            width = 8,
            height = 16
        )

        blitk(
            matrixStack = matrices,
            texture = heightGrid,
            x = pX + 1,
            y = pY,
            width = POKEMON_PORTRAIT_WIDTH,
            height = POKEMON_DESCRIPTION_HEIGHT
        )

        blitk(
            matrixStack = context.matrices,
            texture = scrollBorder,
            x = (pX + 1) / SCALE,
            y = pY / SCALE,
            width = 274,
            height = 4,
            textureHeight = 8,
            vOffset = 0,
            scale = SCALE
        )

        blitk(
            matrixStack = context.matrices,
            texture = scrollBorder,
            x = (pX + 1) / SCALE,
            y = (pY + 40) / SCALE,
            width = 274,
            height = 4,
            textureHeight = 8,
            vOffset = 4,
            scale = SCALE
        )

        matrices.pop()
    }
}