package com.cobblemon.mod.common.api.riding.behaviour.types

import com.bedrockk.molang.Expression
import com.bedrockk.molang.runtime.MoLangMath.lerp
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.riding.behaviour.*
import com.cobblemon.mod.common.api.riding.posing.PoseOption
import com.cobblemon.mod.common.api.riding.posing.PoseProvider
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.*
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.SmoothDouble
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.math.*

class BirdAirBehaviour : RidingBehaviour<BirdAirSettings, BirdAirState> {
    companion object {
        val KEY = cobblemonResource("air/bird")
    }

    override val key: ResourceLocation = KEY

    val poseProvider = PoseProvider<BirdAirSettings, BirdAirState>(PoseType.HOVER)
        .with(PoseOption(PoseType.FLY) { _, state, _ -> state.rideVel.get().z > 0.1 })

    override fun isActive(settings: BirdAirSettings, state: BirdAirState, vehicle: PokemonEntity): Boolean {
        return true
    }

    override fun pose(settings: BirdAirSettings, state: BirdAirState, vehicle: PokemonEntity): PoseType {
        return poseProvider.select(settings, state, vehicle)
    }

    override fun speed(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        return state.rideVel.get().length().toFloat()
    }

    override fun rotation(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity,
        driver: LivingEntity
    ): Vec2 {
        return Vec2(driver.xRot * 0.5f, driver.yRot)
    }

    override fun velocity(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity,
        driver: Player,
        input: Vec3
    ): Vec3 {
        var yVel = 0.0
        var leftForce = 0.0
        var upForce = 0.0
        var forwardForce = 0.0

        //Perform ride velocity update
        calculateRideSpaceVel(settings, state, vehicle, driver)

        //Translate ride space velocity to world space velocity.
        val controller = (driver as? OrientationControllable)?.orientationController
        if (controller != null) {
            //Need to deadzone this when straight up or down

            upForce += -1.0 * sin(Math.toRadians(controller.pitch.toDouble())) * state.rideVel.get().z
            forwardForce += cos(Math.toRadians(controller.pitch.toDouble())) * state.rideVel.get().z

            upForce += cos(Math.toRadians(controller.pitch.toDouble())) * state.rideVel.get().y
            forwardForce += sin(Math.toRadians(controller.pitch.toDouble())) * state.rideVel.get().y
        }


        //Bring the ride out of the sky when stamina is depleted.
        if (state.stamina.get() <= 0.0) {
            upForce -= 0.3
        }

        val altitudeLimit = vehicle.runtime.resolveDouble(settings.altitudeExpr)

        //Only limit altitude if altitude is not infinite
        if (!vehicle.runtime.resolveBoolean(settings.infiniteAltitude)) {
            //Provide a hard limit on altitude
            upForce = if (vehicle.y >= altitudeLimit && upForce > 0) 0.0 else upForce
        }

        val velocity = Vec3(state.rideVel.get().x , upForce, forwardForce)
        return velocity
    }

    /*
    *  Calculates the change in the ride space vector due to player input and ride state
    */
    fun calculateRideSpaceVel(settings: BirdAirSettings, state: BirdAirState, vehicle: PokemonEntity, driver: Player) {
        //retrieve stats
        val topSpeed = vehicle.runtime.resolveDouble(settings.topSpeedExpr)
        val glideTopSpeed = vehicle.runtime.resolveDouble(settings.glideTopSpeedExpr)
        val accel = vehicle.runtime.resolveDouble(settings.accelExpr)
        val staminaStat = vehicle.runtime.resolveDouble(settings.staminaExpr)

        var glideSpeedChange = 0.0

        val currSpeed = state.rideVel.get().length()

        //Flag for determining if player is actively inputting
        var activeInput = false

        var newVelocity = Vec3(state.rideVel.get().x, state.rideVel.get().y, state.rideVel.get().z)

        //speed up and slow down based on input
        if (driver.zza != 0.0f && state.stamina.get() > 0.0) {
            //make sure it can't exceed top speed
            val forwardInput = when {
                driver.zza > 0 && newVelocity.z > topSpeed -> 0.0
                driver.zza < 0 && newVelocity.z < (-topSpeed / 3.0) -> 0.0
                else -> driver.zza.sign
            }

            newVelocity = Vec3(
                newVelocity.x,
                newVelocity.y,
                (newVelocity.z + (accel * forwardInput.toDouble())))

            activeInput = true
        }

        val controller = (driver as? OrientationControllable)?.orientationController
        if (controller != null) {
            //Base glide speed change on current pitch of the ride.
            glideSpeedChange = sin(Math.toRadians(controller.pitch.toDouble()))
            glideSpeedChange = glideSpeedChange.pow(3) * 0.5

            //TODO: Possibly create a deadzone around parallel where glide doesn't affect speed?
            if (glideSpeedChange <= 0.0) {
                //Ensures that a propelling force is still able to be applied when
                //climbing in height
                if (driver.zza <= 0) {
                    //speed decrease should be 2x speed increase?
                    //state.currSpeed = max(state.currSpeed + (0.0166) * glideSpeedChange, 0.0 )
                    newVelocity = Vec3(
                        newVelocity.x,
                        newVelocity.y,
                        lerp( newVelocity.z, 0.0,glideSpeedChange * -0.0166 * 2 )
                    )
                }
            } else {
                // only add to the speed if it hasn't exceeded the current
                //glide angles maximum amount of speed that it can give.
                //state.currSpeed = min(state.currSpeed + ((0.0166 * 2) * glideSpeedChange), maxGlideSpeed)
                newVelocity = Vec3(
                    newVelocity.x,
                    newVelocity.y,
                    min(newVelocity.z + ((0.0166 * 2) * glideSpeedChange), glideTopSpeed)
                )
            }
        }

        //Lateral movement based on driver input.
        val latTopSpeed = topSpeed / 2.0
        if (driver.xxa != 0.0f && state.stamina.get() > 0.0) {
            newVelocity = Vec3(
                (newVelocity.x + (accel * driver.xxa)).coerceIn(-latTopSpeed, latTopSpeed),
                newVelocity.y,
                newVelocity.z)
            activeInput = true
        }
        else {
            newVelocity = Vec3(
                lerp(newVelocity.x, 0.0, latTopSpeed / 20.0),
                newVelocity.y,
                newVelocity.z)
        }

        //Vertical movement based on driver input.
        val vertTopSpeed = topSpeed / 2.0
        val vertInput = when {
            driver.jumping -> 1.0
            driver.isShiftKeyDown -> -1.0
            else -> 0.0
        }

        if (vertInput != 0.0 && state.stamina.get() > 0.0) {
            newVelocity = Vec3(
                newVelocity.x,
                (newVelocity.y + (accel * vertInput)).coerceIn(-vertTopSpeed, vertTopSpeed),
                newVelocity.z)
            activeInput = true
        }
        else {
            newVelocity = Vec3(
                newVelocity.x,
                lerp(newVelocity.y, 0.0, vertTopSpeed / 20.0),
                newVelocity.z)
        }

        //Check if the ride should be gliding
        if (driver.isLocalPlayer) {
            if (state.lastGlide.get() + 10 < vehicle.level().gameTime) {
                if (activeInput && state.stamina.get() > 0.0) {
                    state.gliding.set(false)
                } else {
                    state.gliding.set(true)
                }
            }
        }

        //Only perform stamina logic if the ride does not have infinite stamina
        if (!vehicle.runtime.resolveBoolean(settings.infiniteStamina)) {
            if (activeInput) {
                state.stamina.set(state.stamina.get() - (0.05 / staminaStat).toFloat())
            }

            //Lose a base amount of stamina just for being airborne
            state.stamina.set(state.stamina.get() - (0.01 / staminaStat).toFloat())
        }
        else
        {
            state.stamina.set(1.0f)
        }

        //air resistance
        newVelocity = Vec3(
            newVelocity.x,
            newVelocity.y,
            lerp( newVelocity.z,0.0, topSpeed / ( 20.0 * 30.0))
        )
        state.rideVel.set(newVelocity)
    }

    override fun angRollVel(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity,
        driver: Player,
        deltaTime: Double
    ): Vec3 {
        if (driver !is OrientationControllable) return Vec3.ZERO
        val controller = (driver as OrientationControllable).orientationController

        //TODO: Tie into handling
        val handling = vehicle.runtime.resolveDouble(settings.handlingExpr)
        val topSpeed = vehicle.runtime.resolveDouble(settings.topSpeedExpr)
         val rotationChangeRate = 10.0

        var yawForce =  rotationChangeRate * sin(Math.toRadians(controller.roll.toDouble()))
        //for a bit of correction on the rolls limit it to a quarter the amount
        var pitchForce = -0.35 * rotationChangeRate * abs(sin(Math.toRadians(controller.roll.toDouble())))

        //limit rotation modulation when pitched up heavily or pitched down heavily
        yawForce *= abs(cos(Math.toRadians(controller.pitch.toDouble())))
        pitchForce *= abs(cos(Math.toRadians(controller.pitch.toDouble()))) * 1.5

        return Vec3(yawForce, pitchForce, 0.0)
    }

    override fun rotationOnMouseXY(
        settings: BirdAirSettings,
        state: BirdAirState,
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
        val controller = (driver as OrientationControllable).orientationController

        val handling = vehicle.runtime.resolveDouble(settings.handlingExpr)
        val topSpeed = vehicle.runtime.resolveDouble(settings.topSpeedExpr)

        //Smooth out mouse input.
        val smoothingSpeed = 4
        val invertRoll = if (Cobblemon.config.invertRoll) -1 else 1
        val invertPitch = if (Cobblemon.config.invertPitch) -1 else 1
        val xInput = mouseXSmoother.getNewDeltaValue(mouseX * 0.1 * invertRoll, deltaTime * smoothingSpeed);
        val yInput = mouseYSmoother.getNewDeltaValue(mouseY * 0.1 * invertPitch, deltaTime * smoothingSpeed);

        //limit rolling based on handling and current speed.
        //modulated by speed so that when flapping idle in air you are ont wobbling around to look around
        val rotMin = 15.0
        var rollForce = xInput
        val rotLimit = max(handling * normalizeVal(state.rideVel.get().length(), 0.0, topSpeed).pow(3), rotMin)

        //Limit roll by non linearly decreasing inputs towards
        // a rotation limit based on the current distance from
        // that rotation limit
        if (abs(controller.roll + rollForce) < rotLimit) {
            if (sign(rollForce) == sign(controller.roll).toDouble()) {
                val d = abs(abs(controller.roll) - rotLimit)
                rollForce *= (d.pow(2)) / (rotLimit.pow(2))
            }
        } else if (sign(rollForce) == sign(controller.roll).toDouble()) {
            rollForce = 0.0
        }

        //Give the ability to yaw with x mouse input when at low speeds.
        val yawForce = xInput * ( 1.0 - normalizeVal(state.rideVel.get().length(), 0.0, topSpeed).pow(3))

        //yaw, pitch, roll
        return Vec3(yawForce, yInput, rollForce)
    }

    /*
    *  Normalizes the given value between a min and a max.
    *  The result is clamped between 0.0 and 1.0, where 0.0 represents x is at or below min
    *  and 1.0 represents x is at or above it.
    */
    private fun normalizeVal(x: Double, min: Double, max: Double): Double {
        require(max > min) { "max must be greater than min" }
        return ((x - min) / (max - min)).coerceIn(0.0, 1.0)
    }

    override fun canJump(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity,
        driver: Player
    ): Boolean {
        return false
    }

    override fun setRideBar(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        return (state.stamina.get() / 1.0f)
    }

    override fun jumpForce(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity,
        driver: Player,
        jumpStrength: Int
    ): Vec3 {
        return Vec3.ZERO
    }

    override fun gravity(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity,
        regularGravity: Double
    ): Double {
        return 0.0
    }

    override fun rideFovMultiplier(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        val topSpeed = vehicle.runtime.resolveDouble(settings.topSpeedExpr)
        val glideTopSpeed = vehicle.runtime.resolveDouble(settings.glideTopSpeedExpr)

        //Must I ensure that topspeed is greater than minimum?
        val normalizedGlideSpeed = normalizeVal(state.rideVel.get().length(), topSpeed, glideTopSpeed)

        //Only ever want the fov change to be a max of 0.2 and for it to have non linear scaling.
        return 1.0f + normalizedGlideSpeed.pow(2).toFloat() * 0.2f
    }

    override fun useAngVelSmoothing(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun useRidingAltPose(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity,
        driver: Player
    ): Boolean {
        return state.gliding.get()
    }

    override fun inertia(settings: BirdAirSettings, state: BirdAirState, vehicle: PokemonEntity): Double {
        return 0.5
    }

    override fun shouldRoll(settings: BirdAirSettings, state: BirdAirState, vehicle: PokemonEntity): Boolean {
        return true
    }

    override fun turnOffOnGround(settings: BirdAirSettings, state: BirdAirState, vehicle: PokemonEntity): Boolean {
        return false
    }

    override fun dismountOnShift(settings: BirdAirSettings, state: BirdAirState, vehicle: PokemonEntity): Boolean {
        return false
    }

    override fun shouldRotatePokemonHead(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun shouldRotatePlayerHead(
        settings: BirdAirSettings,
        state: BirdAirState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun createDefaultState() = BirdAirState()

}

class BirdAirSettings : RidingBehaviourSettings {
    override val key = BirdAirBehaviour.KEY

    //max y level for the ride
    var altitudeExpr: Expression = "q.get_ride_stats('JUMP', 'AIR', 200.0, 128.0)".asExpression()
        private set

    var infiniteAltitude: Expression = "false".asExpression()
        private set

    var infiniteStamina: Expression = "false".asExpression()
        private set

    var handlingExpr: Expression = "q.get_ride_stats('SKILL', 'AIR', 135.0, 45.0)".asExpression()
        private set

    var topSpeedExpr: Expression = "q.get_ride_stats('SPEED', 'AIR', 1.0, 0.35)".asExpression()
        private set

    var glideTopSpeedExpr: Expression = "q.get_ride_stats('SPEED', 'AIR', 2.0, 1.0)".asExpression()
        private set

    // Max accel is a whole 1.0 in 3 seconds. The conversion in the function below is to convert seconds to ticks
    var accelExpr: Expression = "q.get_ride_stats('ACCELERATION', 'AIR', (1.0 / (20.0 * 3.0)), (1.0 / (20.0 * 8.0)))".asExpression()
        private set

    //Seconds self propelled flight (glide is not self propelled in this case)
    var staminaExpr: Expression = "q.get_ride_stats('STAMINA', 'AIR', 120.0, 20.0)".asExpression()
        private set

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeExpression(altitudeExpr)
        buffer.writeExpression(infiniteAltitude)
        buffer.writeExpression(infiniteStamina)
        buffer.writeExpression(handlingExpr)
        buffer.writeExpression(topSpeedExpr)
        buffer.writeExpression(glideTopSpeedExpr)
        buffer.writeExpression(accelExpr)
        buffer.writeExpression(staminaExpr)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf) {
        altitudeExpr = buffer.readExpression()
        infiniteAltitude = buffer.readExpression()
        infiniteStamina = buffer.readExpression()
        handlingExpr = buffer.readExpression()
        topSpeedExpr = buffer.readExpression()
        glideTopSpeedExpr = buffer.readExpression()
        accelExpr = buffer.readExpression()
        staminaExpr = buffer.readExpression()
    }
}

class BirdAirState : RidingBehaviourState {
    var rideVel = ridingState(Vec3.ZERO, Side.CLIENT)
    var stamina = ridingState(1.0f, Side.CLIENT)
    var gliding = ridingState(false, Side.CLIENT)
    var lastGlide = ridingState(-100L, Side.CLIENT)

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeVec3(rideVel.get())
        buffer.writeFloat(stamina.get())
        buffer.writeBoolean(gliding.get())
    }

    override fun decode(buffer: RegistryFriendlyByteBuf) {
        rideVel.set(buffer.readVec3(), forced = true)
        stamina.set(buffer.readFloat(), forced = true)
        gliding.set(buffer.readBoolean(), forced = true)
    }

    override fun reset() {
        rideVel.set(Vec3.ZERO, forced = true)
        stamina.set(1.0f, forced = true)
        gliding.set(false, forced = true)
        lastGlide.set(-100L, forced = true)
    }

    override fun toString(): String {
        return "BirdAirState(rideVel=${rideVel.get()}, stamina=${stamina.get()}, gliding=${gliding.get()})"
    }

    override fun copy() = BirdAirState().also {
        it.rideVel.set(this.rideVel.get(), forced = true)
        it.stamina.set(this.stamina.get(), forced = true)
        it.gliding.set(this.gliding.get(), forced = true)
        it.lastGlide.set(this.lastGlide.get(), forced = true)
    }

    override fun shouldSync(previous: RidingBehaviourState): Boolean {
        if (previous !is BirdAirState) return false
        if (previous.rideVel != rideVel) return true
        if (previous.stamina != stamina) return true
        if (previous.gliding != gliding) return true
        return false
    }
}
