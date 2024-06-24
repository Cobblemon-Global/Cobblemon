/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex.widgets

import com.cobblemon.mod.common.api.gui.MultiLineLabelK
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.ScrollingWidget
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.HALF_OVERLAY_WIDTH
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.POKEMON_DESCRIPTION_HEIGHT
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.POKEMON_DESCRIPTION_PADDING
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCALE
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCROLL_BAR_WIDTH
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper

abstract class InfoTextScrollWidget(val pX: Int, val pY: Int): ScrollingWidget<InfoTextScrollWidget.TextSlot>(
    left = pX,
    top = pY,
    width = HALF_OVERLAY_WIDTH,
    height = POKEMON_DESCRIPTION_HEIGHT
) {
    companion object {
        private val scrollBorder = cobblemonResource("textures/gui/pokedex/info_scroll_border.png")
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        blitk(
            matrixStack = context.matrices,
            texture = scrollBorder,
            x = (pX + 1) / SCALE,
            y = (pY + 40) / SCALE,
            width = 266,
            height = 4,
            textureHeight = 8,
            vOffset = 4,
            scale = SCALE
        )
    }
    override fun addEntry(entry: TextSlot): Int {
        return super.addEntry(entry)
    }

    fun setText(text: Collection<String>) {
        clearEntries()
        text.forEach {
            MinecraftClient.getInstance().textRenderer.textHandler.wrapLines(
                it.text(),
                ((width - SCROLL_BAR_WIDTH  - (POKEMON_DESCRIPTION_PADDING * 2)) / SCALE).toInt(),
                Style.EMPTY
            ).stream()
                .map { it.string }
                .forEach { addEntry(TextSlot(it)) }
        }
    }

    override fun renderScrollbar(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val xLeft = this.scrollbarPositionX
        val xRight = xLeft + 3

        val barHeight = this.bottom - this.top

        var yBottom = ((barHeight * barHeight).toFloat() / this.maxPosition.toFloat()).toInt()
        yBottom = MathHelper.clamp(yBottom, 32, barHeight - 8)
        var yTop = scrollAmount.toInt() * (barHeight - yBottom) / this.maxScroll + this.top
        if (yTop < this.top) {
            yTop = this.top
        }

        context.fill(xLeft + 1, this.top + 1, xRight - 1, this.bottom - 1, ColorHelper.Argb.getArgb(255, 126, 231, 229)) // background
        context.fill(xLeft,yTop + 1, xRight, yTop + yBottom - 1, ColorHelper.Argb.getArgb(255, 58, 150, 182)) // base
    }
    override fun getScrollbarPositionX(): Int {
        return left + width - scrollBarWidth
    }

    class TextSlot(val text : String) : Slot<TextSlot>() {
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
            val matrices = context.matrices

            matrices.push()
            MultiLineLabelK.create(
                component = text.text(),
                width = (139 - SCROLL_BAR_WIDTH - (POKEMON_DESCRIPTION_PADDING * 2)) / SCALE,
                maxLines = 30
            ).renderLeftAligned(
                context = context,
                x = x + POKEMON_DESCRIPTION_PADDING,
                y = y + 3,
                YStartOffset = 3,
                ySpacing = 0,
                colour = 0x606B6E,
                scale = SCALE,
                shadow = false
            )
            matrices.pop()
        }

        override fun getNarration(): Text {
            return Text.of(text)
        }
    }
}