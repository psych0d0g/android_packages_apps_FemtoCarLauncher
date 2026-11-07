// AVIN Avant 4 fork: vehicle values published by the head unit's vehicle app
// (com.avin.avant4.car) to Settings.System. Missing keys mean "not an Avant 4".
package io.github.seijikohara.femto.data.vehicle

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

internal data class VehicleState(
    /** Supply voltage in mV. */
    val supplyMv: Int?,
    /** Outside temperature in milli-degrees Celsius (CAN box). */
    val outsideMilliC: Int?,
    /** Low beam on. */
    val lights: Boolean?,
    /** Reverse gear engaged. */
    val reverse: Boolean?,
)

internal object VehicleKeys {
    const val SUPPLY = "avant4_supply_mv"
    const val OUTSIDE = "avant4_outside_mc"
    const val LIGHTS = "avant4_lights"
    const val REVERSE = "avant4_reverse"
    val ALL = listOf(SUPPLY, OUTSIDE, LIGHTS, REVERSE)
}

private fun ContentResolver.intOrNull(key: String): Int? =
    Settings.System
        .getString(this, key)
        ?.toIntOrNull()
        ?.takeIf { it != Int.MIN_VALUE }

private fun ContentResolver.boolOrNull(key: String): Boolean? =
    when (Settings.System.getString(this, key)?.toIntOrNull()) {
        1 -> true
        0 -> false
        else -> null
    }

internal fun ContentResolver.readVehicleState(): VehicleState? {
    if (Settings.System.getString(this, VehicleKeys.SUPPLY) == null) return null
    return VehicleState(
        supplyMv = intOrNull(VehicleKeys.SUPPLY),
        outsideMilliC = intOrNull(VehicleKeys.OUTSIDE),
        lights = boolOrNull(VehicleKeys.LIGHTS),
        reverse = boolOrNull(VehicleKeys.REVERSE),
    )
}

/** Current vehicle state, null on devices without the vehicle app. */
@Composable
internal fun rememberVehicleState(): VehicleState? {
    val resolver = LocalContext.current.contentResolver
    var state by remember { mutableStateOf(resolver.readVehicleState()) }
    DisposableEffect(resolver) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    state = resolver.readVehicleState()
                }
            }
        VehicleKeys.ALL.forEach {
            resolver.registerContentObserver(Settings.System.getUriFor(it), false, observer)
        }
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return state
}
