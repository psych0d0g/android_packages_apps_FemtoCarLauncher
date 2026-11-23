// AVIN Avant 4 fork: reading the vehicle app's Settings.System values.
package io.github.seijikohara.femto.data.vehicle

import android.content.ContentResolver
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VehicleStateTest {
    private val resolver: ContentResolver =
        ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver

    private fun put(
        key: String,
        value: Int,
    ) = Settings.System.putString(resolver, key, value.toString())

    @Test
    fun returns_null_without_vehicle_app() = assertNull(resolver.readVehicleState())

    @Test
    fun doors_and_threshold_absent_on_older_vehicle_app() {
        put(VehicleKeys.SUPPLY, 12_400)
        val state = resolver.readVehicleState()!!
        assertNull(state.doors)
        assertEquals(DEFAULT_SUPPLY_WARN_MV, state.supplyWarnMv)
    }

    @Test
    fun doors_mask_read_from_settings() {
        put(VehicleKeys.SUPPLY, 12_400)
        put(VehicleKeys.DOORS, Doors.DRIVER or Doors.TAILGATE)
        assertEquals(Doors.DRIVER or Doors.TAILGATE, resolver.readVehicleState()!!.doors)
    }

    @Test
    fun doors_null_when_box_reports_none() {
        put(VehicleKeys.SUPPLY, 12_400)
        put(VehicleKeys.DOORS, Int.MIN_VALUE)
        assertNull(resolver.readVehicleState()!!.doors)
    }

    @Test
    fun doors_parse_rejects_negative_and_masks_unknown_bits() {
        assertNull(Doors.parse(-1))
        assertNull(Doors.parse(null))
        assertEquals(Doors.PASSENGER, Doors.parse(Doors.PASSENGER or 0x40))
    }

    @Test
    fun warn_threshold_read_from_settings() {
        put(VehicleKeys.SUPPLY, 12_100)
        put(VehicleKeys.SUPPLY_WARN, 12_200)
        val state = resolver.readVehicleState()!!
        assertEquals(12_200, state.supplyWarnMv)
        assertTrue(state.supplyLow)
    }

    @Test
    fun supply_low_uses_default_threshold() {
        assertFalse(VehicleState(supplyMv = 11_800, outsideMilliC = null, lights = null, reverse = null).supplyLow)
        assertTrue(VehicleState(supplyMv = 11_799, outsideMilliC = null, lights = null, reverse = null).supplyLow)
        assertEquals(DEFAULT_SUPPLY_WARN_MV, parseSupplyWarnMv(0))
    }
}
