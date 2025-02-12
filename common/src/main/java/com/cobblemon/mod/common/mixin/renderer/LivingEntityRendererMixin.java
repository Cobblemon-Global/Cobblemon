/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.renderer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @author landonjw
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
//    @Redirect(
//            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasVehicle()Z"),
//            slice = @Slice(
//                    from = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"),
//                    to = @At(value = "TAIL")
//            )
//    )
//    private boolean dodgeRenderer(LivingEntity instance) {
//        return false;
//    }

    /*@WrapOperation(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;wrapDegrees(F)F")
    )
    private float replaceWrapValue(float angle, Operation<Float> original){
        if(origin){
            return 0f;
        } else {
            return original.call(angle);
        }
    }*/
}