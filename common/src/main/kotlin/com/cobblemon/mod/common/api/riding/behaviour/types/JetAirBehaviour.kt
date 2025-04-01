package com.cobblemon.mod.common.api.riding.behaviour.types

import com.bedrockk.molang.Expression
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviour
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourSettings
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourState
import com.cobblemon.mod.common.api.riding.posing.PoseOption
import com.cobblemon.mod.common.api.riding.posing.PoseProvider
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.*
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.util.SmoothDouble
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import kotlin.math.*

class JetAirBehaviour : RidingBehaviour<JetAirSettings, JetAirState> {
    companion object {
        val KEY = cobblemonResource("air/jet")
    }

    val poseProvider = PoseProvider(PoseType.HOVER)
        .with(PoseOption(PoseType.FLY) { it.entityData.get(PokemonEntity.MOVING) })

    override fun isActive(settings: JetAirSettings, state: JetAirState, vehicle: PokemonEntity): Boolean {
        return true
    }

    override fun pose(settings: JetAirSettings, state: JetAirState, vehicle: PokemonEntity): PoseType {
        return poseProvider.select(vehicle)
    }

    override fun speed(settings: JetAirSettings, state: JetAirState, vehicle: PokemonEntity, driver: Player): Float {
        //retrieve stats
        val topSpeed = vehicle.runtime.resolveDouble(settings.topSpeedExpr)
        val staminaStat = vehicle.runtime.resolveDouble(settings.staminaExpr)

        //retrieve minSpeed
        val minSpeed = vehicle.runtime.resolveDouble(settings.minSpeed)

        //Reduce stamina unless stamina is infinite
        if(!vehicle.runtime.resolveBoolean(settings.infiniteStamina)) {
            //Calculate stamina loss due to speed
            //At max speed it will tick down 0.1 a second so the stamina will last ten seconds
            //There has got to be a better way to express this equation. It interpolates between 0.5 and 1.0
            var staminaRate = (normalizeSpeed(state.rideVel.length(), minSpeed, topSpeed))

            //interpolate between 0.25 and 1.0 so that you always have at least a min of 0.25 stam loss
            staminaRate = 0.25 + (0.75 * staminaRate.pow(3))

            //Calculate stamina loss in seconds achievable at top speed
            val staminaLoss = staminaRate * (1.0 / (20.0 * staminaStat))
            state.stamina = max(state.stamina - staminaLoss, 0.0).toFloat()
        }
        else{
            state.stamina = 1.0f
        }

        return state.rideVel.length().toFloat()
    }

    //TODO: Move these functions to a riding util class.
    /*
    *  Normalizes the current speed between minSpeed and maxSpeed.
    *  The result is clamped between 0.0 and 1.0, where 0.0 represents minSpeed and 1.0 represents maxSpeed.
    */
    private fun normalizeSpeed(currSpeed: Double, minSpeed: Double, maxSpeed: Double): Double {
        require(maxSpeed > minSpeed) { "maxSpeed must be greater than minSpeed" }
        return ((currSpeed - minSpeed) / (maxSpeed - minSpeed)).coerceIn(0.0, 1.0)
    }

    override fun rotation(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity,
        driver: LivingEntity
    ): Vec2 {
        return Vec2(driver.xRot * 0.5f, driver.yRot)
    }

    override fun velocity(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity,
        driver: Player,
        input: Vec3
    ): Vec3 {
        var upForce = 0.0
        var forwardForce = 0.0

        val controller = (driver as? OrientationControllable)?.orientationController

        //Calculate ride space velocity
        calculateRideSpaceVel(settings, state, vehicle, driver)

        //Translate ride space velocity to world space velocity.
        if(controller != null) {
            upForce =  -1.0 * sin(Math.toRadians(controller.pitch.toDouble())) * state.rideVel.z
            forwardForce =  cos(Math.toRadians(controller.pitch.toDouble())) * state.rideVel.z
        }

        //If stamina has run out then initiate forced glide down.
        upForce = if(state.stamina > 0.0) upForce else -0.7

        val altitudeLimit = vehicle.runtime.resolveDouble(settings.jumpExpr)

        //Only limit altitude if altitude is not infinite
        if (!vehicle.runtime.resolveBoolean(settings.infiniteAltitude)) {
            //Provide a hard limit on altitude
            upForce = if (vehicle.y >= altitudeLimit && upForce > 0) 0.0 else upForce
        }

        val velocity = Vec3(0.0 , upForce, forwardForce )

        return velocity
    }

    /*
    *  Calculates the change in the ride space vector due to player input and ride state
    */
    private fun calculateRideSpaceVel(settings: JetAirSettings, state: JetAirState, vehicle: PokemonEntity, driver: Player){
        val topSpeed = vehicle.runtime.resolveDouble(settings.topSpeedExpr)
        val accel = vehicle.runtime.resolveDouble(settings.accelExpr)
        val altitudeLimit = vehicle.runtime.resolveDouble(settings.jumpExpr)
        val minSpeed = vehicle.runtime.resolveDouble(settings.minSpeed)
        val speed = state.rideVel.length()

        //Give no altitude limit if at max jump stat.
        val pushingHeightLimit = if(vehicle.runtime.resolveBoolean(settings.infiniteStamina)) false
        else (vehicle.y >= altitudeLimit && vehicle.xRot <= 0)


        //speed up and slow down based on input
        if( driver.zza > 0.0 && speed < topSpeed && state.stamina > 0.0f && !pushingHeightLimit) {
            //modify acceleration to be slower when at closer speeds to top speed
            val accelMod = max( -(normalizeSpeed(speed, minSpeed, topSpeed)) + 1, 0.0)
            state.rideVel = Vec3(state.rideVel.x, state.rideVel.y, min( state.rideVel.z + (accel * accelMod) , topSpeed))
        }
        else if( driver.zza >= 0.0 && (state.stamina == 0.0f || pushingHeightLimit) ) {
            state.rideVel = Vec3(state.rideVel.x, state.rideVel.y, max( state.rideVel.z - ((accel) / 4) , minSpeed))
        }
        else if ( driver.zza < 0.0 && speed > minSpeed) {
            //modify deccel to be slower when at closer speeds to minimum speed
            val deccelMod = max( (normalizeSpeed(speed, minSpeed, topSpeed) - 1).pow(2), 0.1)

            //Decelerate currently always a constant half of max acceleration.
            state.rideVel = Vec3(state.rideVel.x, state.rideVel.y, max( state.rideVel.z - ((accel * deccelMod) / 2) , minSpeed))
        }
    }

    override fun angRollVel(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity,
        driver: Player,
        deltaTime: Double
    ): Vec3 {
        //Cap at a rate of 5fps so frame skips dont lead to huge jumps
        val cappedDeltaTime = min( deltaTime, 0.2)

        //Get handling in degrees per second
        val yawRotRate = vehicle.runtime.resolveDouble(settings.handlingYawExpr)

        //Base the change off of deltatime.
        var handlingYaw = yawRotRate * (cappedDeltaTime)

        //apply stamina debuff if applicable
        handlingYaw *= if(state.stamina > 0.0) 1.0 else 0.5

        //A+D to yaw
        val yawForce = driver.xxa * handlingYaw * -1

        return Vec3( yawForce, 0.0, 0.0)
    }

    override fun rotationOnMouseXY(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity,
        driver: Player,
        mouseY: Double,
        mouseX: Double,
        mouseYSmoother: SmoothDouble,
        mouseXSmoother: SmoothDouble,
        sensitivity: Double,
        deltaTime: Double
    ): Vec3 {
        if(driver !is OrientationControllable) return Vec3.ZERO
        //TODO: figure out a cleaner solution to this issue of large jumps when skipping frames or lagging
        //Cap at a rate of 5fps so frame skips dont lead to huge jumps
        val cappedDeltaTime = min( deltaTime, 0.2)

        val invertRoll = if (Cobblemon.config.invertRoll) -1 else 1
        val invertPitch = if (Cobblemon.config.invertPitch) -1 else 1

        // Accumulate the mouse input
        state.currMouseXForce = (state.currMouseXForce + (0.0015 * mouseX * invertRoll)).coerceIn(-1.0, 1.0)
        state.currMouseYForce = (state.currMouseYForce + (0.0015 * mouseY * invertPitch)).coerceIn(-1.0, 1.0)

        //Get handling in degrees per second
        var handling = vehicle.runtime.resolveDouble(settings.handlingExpr)

        //convert it to delta time
        handling *= (cappedDeltaTime)

        //apply stamina debuff if applicable
        handling *= if(state.stamina > 0.0) 1.0 else 0.5

        val poke = driver.vehicle as? PokemonEntity

        //TODO: reevaluate if deadzones are needed and if they are still causing issues.
        //create deadzones for the constant input values.
        //val xInput = remapWithDeadzone(state.currMouseXForce, 0.025, 1.0)
        //val yInput = remapWithDeadzone(state.currMouseYForce, 0.025, 1.0)

        val pitchRot = handling * state.currMouseYForce

        //Roll is 1.5 times as fast as pitch
        val rollRot =  handling * 1.5 * state.currMouseXForce

        //yaw, pitch, roll
        return Vec3(0.0, pitchRot,  rollRot)
    }

    override fun canJump(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity,
        driver: Player
    ): Boolean {
        return false
    }

    override fun setRideBar(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        return (state.stamina / 1.0f)
    }

    override fun jumpForce(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity,
        driver: Player,
        jumpStrength: Int
    ): Vec3 {
        return Vec3.ZERO
    }

    override fun gravity(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity,
        regularGravity: Double
    ): Double {
        return 0.0
    }

    override fun rideFovMultiplier(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        val topSpeed = vehicle.runtime.resolveDouble(settings.topSpeedExpr)
        val minSpeed = vehicle.runtime.resolveDouble(settings.minSpeed)

        //Must I ensure that topspeed is greater than minimum?
        val normalizedSpeed = normalizeSpeed(state.rideVel.length(), minSpeed, topSpeed)

        //TODO: Determine if this should be based on max possible speed instead of top speed.
        //Only ever want the fov change to be a max of 0.2 and for it to have non linear scaling.
        return 1.0f + normalizedSpeed.pow(2).toFloat() * 0.2f
    }

    override fun useAngVelSmoothing(settings: JetAirSettings, state: JetAirState, vehicle: PokemonEntity): Boolean {
        return true
    }

    override fun useRidingAltPose(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity,
        driver: Player
    ): Boolean {
        return false
    }

    override fun inertia(settings: JetAirSettings, state: JetAirState, vehicle: PokemonEntity): Double {
        return 0.5
    }

    override fun shouldRoll(settings: JetAirSettings, state: JetAirState, vehicle: PokemonEntity): Boolean {
        return true
    }

    override fun turnOffOnGround(settings: JetAirSettings, state: JetAirState, vehicle: PokemonEntity): Boolean {
        return false
    }

    override fun dismountOnShift(settings: JetAirSettings, state: JetAirState, vehicle: PokemonEntity): Boolean {
        return false
    }

    override fun shouldRotatePokemonHead(
        settings: JetAirSettings,
        state: JetAirState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun shouldRotatePlayerHead(settings: JetAirSettings, state: JetAirState, vehicle: PokemonEntity): Boolean {
        return false
    }

    override fun createDefaultState() = JetAirState()
}

class JetAirSettings : RidingBehaviourSettings {
    override val key = JetAirBehaviour.KEY

    var gravity: Expression = "0".asExpression()
        private set

    var minSpeed: Expression = "1.2".asExpression()
        private set

    var handlingExpr: Expression = "q.get_ride_stats('SKILL', 'AIR', 140.0, 20.0)".asExpression()
        private set

    var handlingYawExpr: Expression = "q.get_ride_stats('SKILL', 'AIR', 25.0, 8.0)".asExpression()
        private set

    var topSpeedExpr: Expression = "q.get_ride_stats('SPEED', 'AIR', 2.5, 1.0)".asExpression()
        private set

    // Max accel is a whole 1.0 in 1 second. The conversion in the function below is to convert seconds to ticks
    var accelExpr: Expression = "q.get_ride_stats('ACCELERATION', 'AIR', (1.0 / (20.0 * 1.0)), (1.0 / (20.0 * 5.0)))".asExpression()
        private set

    // Between 60 seconds and 10 seconds at the lowest when at full speed.
    var staminaExpr: Expression = "q.get_ride_stats('STAMINA', 'AIR', 60.0, 10.0)".asExpression()
        private set

    var jumpExpr: Expression = "q.get_ride_stats('JUMP', 'AIR', 300.0, 128.0)".asExpression()
        private set

    // Make configurable by json
    var infiniteStamina: Expression = "false".asExpression()
        private set

    var infiniteAltitude: Expression = "false".asExpression()
        private set

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeExpression(gravity)
        buffer.writeExpression(minSpeed)
        buffer.writeExpression(handlingExpr)
        buffer.writeExpression(handlingYawExpr)
        buffer.writeExpression(topSpeedExpr)
        buffer.writeExpression(accelExpr)
        buffer.writeExpression(staminaExpr)
        buffer.writeExpression(jumpExpr)
        buffer.writeExpression(infiniteStamina)
        buffer.writeExpression(infiniteAltitude)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf) {
        gravity = buffer.readExpression()
        minSpeed = buffer.readExpression()
        handlingExpr = buffer.readExpression()
        handlingYawExpr = buffer.readExpression()
        topSpeedExpr = buffer.readExpression()
        accelExpr = buffer.readExpression()
        staminaExpr = buffer.readExpression()
        jumpExpr = buffer.readExpression()
        infiniteStamina = buffer.readExpression()
        infiniteAltitude = buffer.readExpression()
    }
}

class JetAirState : RidingBehaviourState {
    override var isDirty = false

    var currSpeed: Double = 0.0
        set(value) {
            field = value
            isDirty = true
        }

    var stamina: Float = 1.0f
        set(value) {
            field = value
            isDirty = true
        }

    var rideVel: Vec3 = Vec3.ZERO
        set(value) {
            field = value
            isDirty = true
        }

    var currMouseXForce: Double = 0.0

    var currMouseYForce: Double = 0.0

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeDouble(currSpeed)
        buffer.writeFloat(stamina)
        buffer.writeVec3(rideVel)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf) {
        currSpeed = buffer.readDouble()
        stamina = buffer.readFloat()
        rideVel = buffer.readVec3()
        isDirty = false
    }

    override fun reset() {
        currSpeed = 0.0
        stamina = 1.0f
        rideVel = Vec3.ZERO
        currMouseXForce = 0.0
        currMouseYForce = 0.0
    }
}
