package com.cobblemon.mod.common.api.riding.behaviour.types

import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviour
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourSettings
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourState
import com.cobblemon.mod.common.api.riding.controller.posing.PoseOption
import com.cobblemon.mod.common.api.riding.controller.posing.PoseProvider
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.riding.controllers.VehicleLandController.Companion.MAXACCEL
import com.cobblemon.mod.common.pokemon.riding.controllers.VehicleLandController.Companion.MAXTOPSPEED
import com.cobblemon.mod.common.pokemon.riding.controllers.VehicleLandController.Companion.MINACCEL
import com.cobblemon.mod.common.pokemon.riding.controllers.VehicleLandController.Companion.MINTOPSPEED
import com.cobblemon.mod.common.util.*
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.util.SmoothDouble
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class VehicleLandBehaviour : RidingBehaviour<VehicleLandSettings, VehicleLandState> {
    companion object {
        val KEY = cobblemonResource("land/vehicle")

        val MAX_TOP_SPEED = 1.0 // 20 bl/s
        val MIN_TOP_SPEED = 0.35 // 7 bl/s
        //val MIN_SPEED = 0.25 // 5 bl/s

        //Accel will lie between 1.0 second and 5.0 seconds
        val MAX_ACCEL = (MAX_TOP_SPEED) / (20*3) //3.0 second to max speed
        val MIN_ACCEL = (MAX_TOP_SPEED) / (20*8) // 8 seconds to max speed

        //Can rotate 90 degrees
        val MAX_HANDLING = 90.0
        val MIN_HANDLING = 30.0


        val MAX_YAW_HANDLING = 16.0
        val MIN_YAW_HANDLING = 8.0
    }

    val poseProvider: PoseProvider = PoseProvider(PoseType.STAND)
        .with(PoseOption(PoseType.WALK) { it.entityData.get(PokemonEntity.MOVING) })

    override fun isActive(settings: VehicleLandSettings, state: VehicleLandState, vehicle: PokemonEntity): Boolean {
        return Shapes.create(vehicle.boundingBox).blockPositionsAsListRounded().any {
            //Need to check other fluids
            if (vehicle.isInWater || vehicle.isUnderWater) {
                return@any false
            }
            //This might not actually work, depending on what the yPos actually is. yPos of the middle of the entity? the feet?
            if (it.y.toDouble() == (vehicle.position().y)) {
                val blockState = vehicle.level().getBlockState(it.below())
                return@any !(!blockState.isAir && blockState.fluidState.isEmpty)
            }
            true
        }
    }

    override fun pose(settings: VehicleLandSettings, state: VehicleLandState, vehicle: PokemonEntity): PoseType {
        return poseProvider.select(vehicle)
    }

    override fun speed(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        val topSpeed = vehicle.getRideStat(RidingStat.SPEED, RidingStyle.AIR, MINTOPSPEED, MAXTOPSPEED)
        val accel = vehicle.getRideStat(RidingStat.ACCELERATION, RidingStyle.AIR, MINACCEL, MAXACCEL)

        //speed up and slow down based on input
        if (driver.zza > 0.0 && state.currSpeed < topSpeed) {
            state.currSpeed = min(state.currSpeed + accel , topSpeed)
        } else if (driver.zza < 0.0 && state.currSpeed > 0.0) {
            //Decelerate is now always a constant half of max acceleration.
            state.currSpeed = max(state.currSpeed - (MAXACCEL / 2), 0.0)
        }

        return state.currSpeed.toFloat()
    }

    override fun rotation(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: LivingEntity
    ): Vec2 {
        val rotationDegrees = driver.xxa * vehicle.runtime.resolveFloat(settings.rotationSpeed)
        val rotation = Vec2(driver.xRot, vehicle.yRot - rotationDegrees)
        state.deltaRotation = Vec2(rotation.x - driver.rotationVector.x, rotation.y - vehicle.rotationVector.y)
        return rotation
    }

    override fun velocity(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player,
        input: Vec3
    ): Vec3 {
        val runtime = vehicle.runtime
        val driveFactor = runtime.resolveFloat(settings.driveFactor)
        var g = driver.zza * driveFactor
        if (g <= 0.0f) {
            g *= runtime.resolveFloat(settings.reverseDriveFactor)
        }
        if (driver.xxa != 0F && g.absoluteValue < runtime.resolveFloat(settings.minimumSpeedToTurn)) {
            driver.xxa = 0F
        }
        val velocity = Vec3(0.0 /* Maybe do drifting here later */, 0.0, g.toDouble() * state.currSpeed)

        return velocity
    }

    override fun angRollVel(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player,
        deltaTime: Double
    ): Vec3 {
        return Vec3.ZERO
    }

    override fun rotationOnMouseXY(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player,
        mouseY: Double,
        mouseX: Double,
        mouseYSmoother: SmoothDouble,
        mouseXSmoother: SmoothDouble,
        sensitivity: Double,
        deltaTime: Double
    ): Vec3 {
        if (driver !is OrientationControllable) return Vec3.ZERO

        //Might need to add the smoothing here for default.
        val invertRoll = if (Cobblemon.config.invertRoll) -1 else 1
        val invertPitch = if (Cobblemon.config.invertPitch) -1 else 1
        return Vec3(0.0, mouseY * invertPitch, mouseX * invertRoll)
    }

    override fun canJump(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player
    ): Boolean {
        return vehicle.runtime.resolveBoolean(settings.canJump)
    }

    override fun setRideBar(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        return 0.0f
    }

    override fun jumpForce(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player,
        jumpStrength: Int
    ): Vec3 {
        val runtime = vehicle.runtime
        runtime.environment.query.addFunction("jump_strength") { DoubleValue(jumpStrength.toDouble()) }
        val jumpVector = settings.jumpVector.map { runtime.resolveFloat(it) }
        return Vec3(jumpVector[0].toDouble(), jumpVector[1].toDouble(), jumpVector[2].toDouble())
    }

    override fun gravity(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        regularGravity: Double
    ): Double {
        return regularGravity
    }

    override fun rideFovMultiplier(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        return 1.0f
    }

    override fun useAngVelSmoothing(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun useRidingAltPose(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player
    ): Boolean {
        return false
    }

    override fun inertia(settings: VehicleLandSettings, state: VehicleLandState, vehicle: PokemonEntity): Double {
        return 0.5
    }

    override fun shouldRoll(settings: VehicleLandSettings, state: VehicleLandState, vehicle: PokemonEntity): Boolean {
        return false
    }

    override fun turnOffOnGround(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun dismountOnShift(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun shouldRotatePokemonHead(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun shouldRotatePlayerHead(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun createDefaultState() = VehicleLandState()
}

class VehicleLandSettings : RidingBehaviourSettings {
    var canJump = "true".asExpression()
        private set

    var jumpVector = listOf("0".asExpression(), "0.3".asExpression(), "0".asExpression())
        private set

    var speed = "0.3".asExpression()
        private set

    var driveFactor = "1.0".asExpression()
        private set

    var reverseDriveFactor = "0.25".asExpression()
        private set

    var minimumSpeedToTurn = "0.1".asExpression()
        private set

    var rotationSpeed = "45/20".asExpression()
        private set

    var lookYawLimit = "101".asExpression()
        private set

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeExpression(canJump)
        buffer.writeExpression(jumpVector[0])
        buffer.writeExpression(jumpVector[1])
        buffer.writeExpression(jumpVector[2])
        buffer.writeExpression(speed)
        buffer.writeExpression(driveFactor)
        buffer.writeExpression(reverseDriveFactor)
        buffer.writeExpression(minimumSpeedToTurn)
        buffer.writeExpression(rotationSpeed)
        buffer.writeExpression(lookYawLimit)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf) {
        canJump = buffer.readExpression()
        jumpVector = listOf(
            buffer.readExpression(),
            buffer.readExpression(),
            buffer.readExpression()
        )
        speed = buffer.readExpression()
        driveFactor = buffer.readExpression()
        reverseDriveFactor = buffer.readExpression()
        minimumSpeedToTurn = buffer.readExpression()
        rotationSpeed = buffer.readExpression()
        lookYawLimit = buffer.readExpression()
    }
}

class VehicleLandState : RidingBehaviourState {
    override var isDirty = false
    var currSpeed = 0.0
    var deltaRotation = Vec2.ZERO

    override fun encode(buffer: RegistryFriendlyByteBuf) = Unit
    override fun decode(buffer: RegistryFriendlyByteBuf) = Unit

    override fun reset() {
        currSpeed = 0.0
    }
}
