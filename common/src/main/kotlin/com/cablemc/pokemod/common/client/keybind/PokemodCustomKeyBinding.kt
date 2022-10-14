/*
 * Copyright (C) 2022 Pokemon Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.client.keybind

import net.minecraft.client.util.InputUtil


/**
 * [PokemodKeyBinding] extension if the action on press shall be something that is not
 * platform-agnostic
 *
 * @author Qu
 * @since 2022-02-17
 */
abstract class PokemodCustomKeyBinding(
    name: String,
    type: InputUtil.Type = InputUtil.Type.KEYSYM,
    key: Int,
    category: String
) : PokemodKeyBinding(name, type, key, category) {

    var run: Runnable? = null

    override fun onPress() {
        run?.run()
    }
}