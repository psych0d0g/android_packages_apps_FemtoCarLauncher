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
    /** Huawei HiLink modem on the head unit (vehicle app), null without a stick. */
    val modem: ModemState? = null,
    /** Open doors as a [Doors] bit mask (CAN box), null when the box reports none. */
    val doors: Int? = null,
    /** Supply voltage below which the card warns, in mV (vehicle app setting). */
    val supplyWarnMv: Int = DEFAULT_SUPPLY_WARN_MV,
) {
    val supplyLow: Boolean get() = supplyMv?.let { it < supplyWarnMv } ?: false
}

/** Resting voltage of a 12 V system worth a glance (weak battery or no charging). */
internal const val DEFAULT_SUPPLY_WARN_MV = 11_800

/** Bits of [VehicleState.doors], as the vehicle app publishes them. The rear window counts as tailgate. */
internal object Doors {
    const val DRIVER = 1
    const val PASSENGER = 2
    const val REAR_LEFT = 4
    const val REAR_RIGHT = 8
    const val TAILGATE = 16
    const val ALL = DRIVER or PASSENGER or REAR_LEFT or REAR_RIGHT or TAILGATE

    /** Mask from the raw setting: negative (-1, Int.MIN_VALUE) means no data. */
    fun parse(raw: Int?): Int? = raw?.takeIf { it >= 0 }?.and(ALL)
}

/** Warning threshold from the raw setting, the default when missing or implausible. */
internal fun parseSupplyWarnMv(raw: Int?): Int = raw?.takeIf { it > 0 } ?: DEFAULT_SUPPLY_WARN_MV

/** Cellular stick as reported by the vehicle app: "type|operator|level 0..5|R". */
internal data class ModemState(
    val type: String,
    val operator: String,
    val level: Int,
    val roaming: Boolean,
)

internal fun parseModem(value: String?): ModemState? {
    val parts = value?.split('|') ?: return null
    if (parts.size < 3) return null
    return ModemState(
        type = parts[0],
        operator = parts[1],
        level = parts[2].toIntOrNull()?.coerceIn(0, 5) ?: 0,
        roaming = parts.getOrNull(3) == "R",
    )
}

internal object VehicleKeys {
    const val SUPPLY = "avant4_supply_mv"
    const val OUTSIDE = "avant4_outside_mc"
    const val LIGHTS = "avant4_lights"
    const val REVERSE = "avant4_reverse"
    const val MODEM = "avant4_modem_info"
    const val DOORS = "avant4_doors"
    const val SUPPLY_WARN = "avant4_supply_warn_mv"
    val ALL = listOf(SUPPLY, OUTSIDE, LIGHTS, REVERSE, MODEM, DOORS, SUPPLY_WARN)
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
        modem = parseModem(Settings.System.getString(this, VehicleKeys.MODEM)),
        doors = Doors.parse(intOrNull(VehicleKeys.DOORS)),
        supplyWarnMv = parseSupplyWarnMv(intOrNull(VehicleKeys.SUPPLY_WARN)),
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
