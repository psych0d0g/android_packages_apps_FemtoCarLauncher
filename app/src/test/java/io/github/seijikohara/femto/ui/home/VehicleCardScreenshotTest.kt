// AVIN Avant 4 fork: the vehicle card in the weather slot on the head unit's 1024x600 display.
package io.github.seijikohara.femto.ui.home

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.seijikohara.femto.data.vehicle.Doors
import io.github.seijikohara.femto.data.vehicle.VehicleKeys
import io.github.seijikohara.femto.testfixtures.DashboardFixtures
import io.github.seijikohara.femto.testfixtures.DashboardGeometries
import io.github.seijikohara.femto.testfixtures.MapBackdrop
import io.github.seijikohara.femto.testfixtures.RobolectricHaze
import io.github.seijikohara.femto.testfixtures.ScreenshotCompareOptions
import io.github.seijikohara.femto.ui.home.components.DashboardScaffold
import io.github.seijikohara.femto.ui.home.components.GlassConfig
import io.github.seijikohara.femto.ui.home.components.MapConfig
import io.github.seijikohara.femto.ui.home.components.PanelVisibility
import io.github.seijikohara.femto.ui.locale.SpeedUnit
import io.github.seijikohara.femto.ui.locale.TemperatureUnit
import io.github.seijikohara.femto.ui.theme.FemtoTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The card reads the vehicle app's Settings.System keys itself, so each case
 * seeds them and renders the whole dashboard like [DashboardScreenshotTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = DashboardGeometries.BUDGET_1024X600)
class VehicleCardScreenshotTest {
    @Test
    fun vehicle_card_doors_closed() = capture("closed", doors = 0)

    // Worst case for the card height: doors open with reverse gear engaged.
    @Test
    fun vehicle_card_doors_open() =
        capture("open", doors = Doors.DRIVER or Doors.REAR_RIGHT or Doors.TAILGATE, reverse = true)

    @Test
    fun vehicle_card_door_open_single_dark() = capture("open-single-dark", doors = Doors.PASSENGER, darkTheme = true)

    // Older vehicle app or no door frames from the CAN box: no glyph, low supply above a raised threshold.
    @Test
    fun vehicle_card_without_doors() = capture("no-doors", doors = Int.MIN_VALUE, supplyMv = 12_100, warnMv = 12_200)

    private fun capture(
        name: String,
        doors: Int,
        supplyMv: Int = 12_400,
        warnMv: Int? = null,
        darkTheme: Boolean = false,
        reverse: Boolean = false,
    ) {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        mapOf(
            VehicleKeys.SUPPLY to supplyMv,
            VehicleKeys.OUTSIDE to 7_500,
            VehicleKeys.LIGHTS to 1,
            VehicleKeys.REVERSE to if (reverse) 1 else 0,
            VehicleKeys.DOORS to doors,
        ).plus(listOfNotNull(warnMv?.let { VehicleKeys.SUPPLY_WARN to it }))
            .forEach { (key, value) -> Settings.System.putString(resolver, key, value.toString()) }
        captureRoboImage(
            filePath = "src/test/screenshots/vehicle-card-$name.png",
            roborazziOptions = ScreenshotCompareOptions,
        ) {
            RobolectricHaze {
                FemtoTheme(darkTheme = darkTheme) {
                    DashboardScaffold(
                        uiState = DashboardFixtures.state,
                        is24Hour = true,
                        showClockSeconds = true,
                        speedUnit = SpeedUnit.KILOMETERS_PER_HOUR,
                        temperatureUnit = TemperatureUnit.CELSIUS,
                        mapConfig = MapConfig(),
                        panels = PanelVisibility(),
                        glassConfig = GlassConfig(),
                        onAction = {},
                        modifier = Modifier.fillMaxSize(),
                        clock = DashboardFixtures.fixedClock,
                        mapSurface = { _, config -> MapBackdrop(darkTheme = darkTheme, mapConfig = config) },
                    )
                }
            }
        }
    }
}
