/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon

import com.mojang.serialization.Codec
import net.minecraft.util.StringIdentifiable

enum class Gender(val showdownName: String) : StringIdentifiable {
    MALE("M"),
    FEMALE("F"),
    GENDERLESS("N");

    override fun asString(): String = this.name

    companion object {

        @JvmStatic
        val CODEC: Codec<Gender> = StringIdentifiable.createCodec(Gender::values)

    }

}