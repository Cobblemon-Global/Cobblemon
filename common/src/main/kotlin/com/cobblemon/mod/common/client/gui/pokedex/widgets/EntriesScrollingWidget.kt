/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex.widgets

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokedex.ClientPokedex
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokedex.PokedexJSONRegistry
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.gui.ScrollingWidget
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCALE
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCROLL_BASE_HEIGHT
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCROLL_SLOT_SIZE
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCROLL_SLOT_SPACING
import com.cobblemon.mod.common.client.gui.pokedex.widgets.EntriesScrollingWidget.PokemonScrollSlotRow
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.pokedex.DexPokemonData
import com.cobblemon.mod.common.pokemon.RenderablePokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.text.Text
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper
import org.joml.Quaternionf
import org.joml.Vector3f

class EntriesScrollingWidget<PokemonScrollSlotRow : ScrollingWidget.Slot<EntriesScrollingWidget.PokemonScrollSlotRow>>(val pX: Int, val pY: Int, val setPokedexEntry: (DexPokemonData) -> (Unit)
): ScrollingWidget<EntriesScrollingWidget.PokemonScrollSlotRow>(
    width = PokedexGUIConstants.HALF_OVERLAY_WIDTH,
    height = SCROLL_BASE_HEIGHT,
    left = pX,
    top = pY - SCROLL_BASE_HEIGHT,
    slotHeight = SCROLL_SLOT_SIZE + 2
) {

    fun createEntries(filteredPokedex: Collection<DexPokemonData>, clientPokedex: ClientPokedex) {

        filteredPokedex.forEachIndexed { index, _ ->
            if ((index + 5) % 5 == 0) {
                val dexDataList = mutableListOf<DexPokemonData>()
                val discoveryLevelList = mutableListOf<PokedexEntryProgress>()
                for (i in 0..4) {
                    if (index + i < filteredPokedex.size) {
                        val species = filteredPokedex.elementAt(index + i)
                        val discoveryLevel = clientPokedex.speciesEntries[species.identifier]?.highestDiscoveryLevel() ?: PokedexEntryProgress.NONE
                        dexDataList.add(species)
                        discoveryLevelList.add(discoveryLevel)
                    }
                }
                val newEntry = PokemonScrollSlotRow(dexDataList, discoveryLevelList, index == 0) { selectPokemon(it) }
                addEntry(newEntry)
            }
        }
    }

    override fun addEntry(entry: EntriesScrollingWidget.PokemonScrollSlotRow): Int {
        return super.addEntry(entry)
    }

    override fun getScrollbarX(): Int {
        return pX + width - 3// scrollBarWidth
    }

    override fun renderScrollbar(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val xLeft = this.scrollbarX
        val xRight = xLeft + 3

        val barHeight = this.bottom - this.y

        var yBottom = ((barHeight * barHeight).toFloat() / this.maxPosition.toFloat()).toInt()
        yBottom = MathHelper.clamp(yBottom, 32, barHeight - 8)
        var yTop = scrollAmount.toInt() * (barHeight - yBottom) / this.maxScroll + this.y
        if (yTop < this.y) {
            yTop = this.y
        }

        context.fill(xLeft, this.y + 3, xRight, this.bottom - 3, ColorHelper.Argb.getArgb(255, 58, 150, 182)) // background
        context.fill(xLeft,yTop + 3, xRight, yTop + yBottom - 3, ColorHelper.Argb.getArgb(255, 252, 252, 252)) // base
    }

    fun selectPokemon(dexPokemonData: DexPokemonData) {
        setPokedexEntry.invoke(dexPokemonData)
    }

    override fun renderEntry(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        index: Int,
        x: Int,
        y: Int,
        entryWidth: Int,
        entryHeight: Int
    ) {
        val entry =  this.getEntry(index)
        entry.x = x
        entry.y = y
        entry.render(
            context, index, y, x, entryWidth, entryHeight, mouseX, mouseY,
            hoveredEntry == entry, delta
        )
    }

    override fun getEntry(index: Int): EntriesScrollingWidget.PokemonScrollSlotRow {
        return children()[index] as EntriesScrollingWidget.PokemonScrollSlotRow
    }

    class PokemonScrollSlotRow(
        val dexDataList:  MutableList<DexPokemonData>,
        val discoveryLevelList: MutableList<PokedexEntryProgress>,
        val isFirstRow: Boolean = false,
        val setPokedexEntry : (DexPokemonData) -> (Unit)
    ): Slot<PokemonScrollSlotRow>() {
        companion object {
            private val slotResource = cobblemonResource("textures/gui/pokedex/pokedex_slot.png")
            private val slotHighlight = cobblemonResource("textures/gui/pokedex/slot_select.png")
            private val caughtIcon = cobblemonResource("textures/gui/pokedex/caught_icon_small.png")
            private val unknownIcon = cobblemonResource("textures/gui/pokedex/pokedex_slot_unknown.png")
            private val unimplementedIcon = cobblemonResource("textures/gui/pokedex/pokedex_slot_unimplemented.png")
        }

        var x: Int = 0
        var y: Int = 0

        override fun render(
            context: DrawContext,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            dexDataList.forEachIndexed { index, dexData ->
                val state = FloatingState()
                val species = PokemonSpecies.getByIdentifier(dexData.identifier)
                var pokemonNumber = PokedexJSONRegistry.getPokemonVisualDexNumber(dexData)

                if (pokemonNumber.toIntOrNull() != null) {
                    while (pokemonNumber.length < 4) pokemonNumber = "0$pokemonNumber"
                }

                val speciesNumber = pokemonNumber.text()
                val discoveryLevel = discoveryLevelList[index]

                if (species == null) return@forEachIndexed

                val matrices = context.matrices

                val startPosX = x + ((SCROLL_SLOT_SPACING + SCROLL_SLOT_SIZE) * index)
                val startPosY = y + if (isFirstRow) (SCROLL_SLOT_SPACING + 1) else SCROLL_SLOT_SPACING

                blitk(
                    matrixStack = matrices,
                    texture = slotResource,
                    x = startPosX,
                    y = startPosY,
                    width = SCROLL_SLOT_SIZE,
                    height = SCROLL_SLOT_SIZE
                )

                if (getHoveredSlotIndex(mouseX, mouseY) == index) {
                    blitk(
                        matrixStack = matrices,
                        texture = slotHighlight,
                        x = startPosX,
                        y = startPosY,
                        width = SCROLL_SLOT_SIZE,
                        height = SCROLL_SLOT_SIZE,
                        vOffset = SCROLL_SLOT_SIZE,
                        textureHeight = SCROLL_SLOT_SIZE * 2
                    )
                }

                if (discoveryLevel != PokedexEntryProgress.NONE) {
                    context.enableScissor(
                        startPosX + 1,
                        startPosY + 1,
                        startPosX + SCROLL_SLOT_SIZE - 1,
                        startPosY + SCROLL_SLOT_SIZE - 2
                    )
                    matrices.push()
                    matrices.translate(startPosX + (SCROLL_SLOT_SIZE / 2.0), startPosY + 1.0, 0.0)
                    matrices.scale(2.5F, 2.5F, 1F)
                    drawProfilePokemon(
                        renderablePokemon = RenderablePokemon(species, setOf<String>()),
                        matrixStack = matrices,
                        rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(13F, 35F, 0F)),
                        state = state,
                        partialTicks = tickDelta,
                        scale = 4.5F
                    )
                    matrices.pop()
                    context.disableScissor()
                } else {
                    blitk(
                        matrixStack = matrices,
                        texture = unknownIcon,
                        x = startPosX + 8.5,
                        y = startPosY + 9,
                        width = 8,
                        height = 10
                    )

                    if (!species.implemented) {
                        blitk(
                            matrixStack = matrices,
                            texture = unimplementedIcon,
                            x = (startPosX + 14) / SCALE,
                            y = (startPosY + 15.5) / SCALE,
                            width = 7,
                            height = 7,
                            scale = SCALE
                        )
                    }
                }

                // Ensure elements are not hidden behind Pokémon render
                matrices.push()
                matrices.translate(0.0, 0.0, 100.0)

                drawScaledText(
                    context = context,
                    text = speciesNumber,
                    x = startPosX + 1.5,//2,
                    y = startPosY + 2.5,//2
                    shadow = true,
                    scale = SCALE
                )

                if (discoveryLevel == PokedexEntryProgress.CAUGHT) {
                    blitk(
                        matrixStack = matrices,
                        texture = caughtIcon,
                        x = (startPosX + 18) / SCALE,
                        y = (startPosY + 1.5) / SCALE,
                        width = 11,
                        height = 11,
                        scale = SCALE
                    )
                }

                matrices.pop()
            }
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            val hoverIndex = getHoveredSlotIndex(mouseX.toInt(), mouseY.toInt())
            if (hoverIndex > -1 && hoverIndex < dexDataList.size) {
                setPokedexEntry.invoke(dexDataList[hoverIndex])
                MinecraftClient.getInstance().soundManager.play(PositionedSoundInstance.master(CobblemonSounds.PC_CLICK, 1.0F))
            }

            return true
        }

        private fun getHoveredSlotIndex(mouseX: Int, mouseY: Int): Int {
            dexDataList.forEachIndexed { index, _ ->
                val startPosX = x + ((SCROLL_SLOT_SPACING + SCROLL_SLOT_SIZE) * index)
                val startPosY = y + if (isFirstRow) (SCROLL_SLOT_SPACING + 1) else SCROLL_SLOT_SPACING

                if (mouseX in startPosX..(startPosX + SCROLL_SLOT_SIZE)
                    && mouseY in startPosY..(startPosY + SCROLL_SLOT_SIZE)) {
                    return index
                }
            }
            return -1
        }

        override fun getNarration(): Text {
            if (dexDataList.isNotEmpty()) {
                return "${dexDataList[0]}-${dexDataList[dexDataList.size - 1]}".text()
            }
            return "".text()
        }
    }
}