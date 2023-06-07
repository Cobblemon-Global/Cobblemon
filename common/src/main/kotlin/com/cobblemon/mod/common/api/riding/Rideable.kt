/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.riding

import com.cobblemon.mod.common.api.riding.capabilities.RidingCapabilities
import com.cobblemon.mod.common.api.riding.seats.Seat
import net.minecraft.entity.JumpingMount

/**
 * Represents an entity that supports riding.
 *
 * @since 1.5.0
 */
interface Rideable : JumpingMount {

    /**
     * A set of properties denoting how a rideable entity is meant to behave under certain conditions
     *
     * @since 1.5.0
     */
    val properties: RidingProperties

    val capabilities: RidingCapabilities

    /**
     * Specifies a list of stateful seats which are capable of tracking an occupant.
     *
     * @since 1.5.0
     */
    val seats: List<Seat>

}