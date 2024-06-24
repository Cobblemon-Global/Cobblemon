package com.cobblemon.mod.common.client.gui.pokedex.widgets

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.HALF_OVERLAY_HEIGHT
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.HALF_OVERLAY_WIDTH
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCALE
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class SearchWidget(
    val posX: Number,
    val posY: Number,
    width: Int,
    height: Int,
    text: Text = "Search".text(),
    val update: () -> (Unit)): TextFieldWidget(MinecraftClient.getInstance().textRenderer,
    posX.toInt(), posY.toInt(), width, height, text) {

    companion object {
        private val backgroundOverlay = cobblemonResource("textures/gui/pokedex/pokedex_screen_search_overlay.png")
        private val searchIcon = cobblemonResource("textures/gui/pokedex/search_icon.png")
    }

    init {
        this.setMaxLength(24)
        this.setChangedListener {
            update.invoke()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return if (mouseX.toInt() in x..(x + width) && mouseY.toInt() in y..(y + height)) {
            isFocused = true
            true
        } else {
            false
        }
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val matrices = context.matrices

        blitk(
            matrixStack = matrices,
            texture = backgroundOverlay,
            x = posX, y = posY,
            width = HALF_OVERLAY_WIDTH,
            height = HALF_OVERLAY_HEIGHT
        )

        blitk(
            matrixStack = matrices,
            texture = searchIcon,
            x = (posX.toInt() + 3) / SCALE,
            y = (posY.toInt() + 2) / SCALE,
            width = 14,
            height = 14,
            scale = SCALE
        )

//        if(text.isEmpty() && !isFocused) {
//            drawScaledText(
//                context = context,
//                font = CobblemonResources.DEFAULT_LARGE,
//                text = Text.translatable("cobblemon.ui.pokedex.search").bold(),
//                x = posX.toInt() + 13,
//                y = posY.toInt() + 1,
//                shadow = true
//            )
//        }

        if (cursor != text.length) setCursorToEnd(false)

        val input = if (isFocused) "${text}_".text()
            else (if(text.isEmpty()) Text.translatable("cobblemon.ui.pokedex.search") else text.text())

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = input.bold(),
            // text = Text.translatable(if (isFocused) "${text}_" else text).bold(),
            x = posX.toInt() + 13,
            y = posY.toInt() + 1,
            shadow = true
        )
    }
}