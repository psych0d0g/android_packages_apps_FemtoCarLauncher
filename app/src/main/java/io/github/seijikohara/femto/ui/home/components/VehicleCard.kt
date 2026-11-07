// AVIN Avant 4 fork: vehicle diagnosis card in place of the weather card.
package io.github.seijikohara.femto.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.LightbulbOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Thermometer
import com.composables.icons.lucide.Zap
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import io.github.seijikohara.femto.R
import io.github.seijikohara.femto.data.vehicle.VehicleState
import io.github.seijikohara.femto.ui.theme.FemtoDimens
import io.github.seijikohara.femto.ui.theme.FemtoIcon
import io.github.seijikohara.femto.ui.theme.bigNumber
import io.github.seijikohara.femto.ui.theme.glanceCaption
import io.github.seijikohara.femto.ui.theme.normalWeight
import java.util.Locale

// A 12 V system below this resting voltage is worth a glance (weak battery or no charging).
private const val LOW_SUPPLY_MV = 11_800

internal fun formatOutside(milliC: Int?): String =
    milliC?.let { String.format(Locale.getDefault(), "%+.1f°", it / 1000.0) } ?: "—"

internal fun formatSupply(mv: Int?): String =
    mv?.let { String.format(Locale.getDefault(), "%.1f V", it / 1000.0) } ?: "—"

@Composable
internal fun VehicleCard(
    state: VehicleState,
    modifier: Modifier = Modifier,
    hazeState: HazeState = rememberHazeState(),
    glassConfig: GlassConfig = GlassConfig(),
) = Surface(
    modifier = modifier.glassChrome(MaterialTheme.shapes.large, hazeState, glassConfig),
    shape = MaterialTheme.shapes.large,
    color = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(FemtoDimens.CardPaddingCompact),
        verticalArrangement = Arrangement.spacedBy(FemtoDimens.CardSectionGapCompact),
    ) {
        // Hero: outside temperature, the value a driver glances for.
        Row(verticalAlignment = Alignment.CenterVertically) {
            FemtoIcon(
                imageVector = Lucide.Thermometer,
                contentDescription = stringResource(R.string.vehicle_outside),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = formatOutside(state.outsideMilliC),
                style = MaterialTheme.typography.bigNumber(
                    size = FemtoDimens.Text4Xl,
                    weight = MaterialTheme.typography.normalWeight,
                ),
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        VehicleRow(
            icon = Lucide.Zap,
            label = stringResource(R.string.vehicle_supply),
            value = formatSupply(state.supplyMv),
            warn = state.supplyMv?.let { it < LOW_SUPPLY_MV } ?: false,
        )
        VehicleRow(
            icon = if (state.lights == true) Lucide.Lightbulb else Lucide.LightbulbOff,
            label = stringResource(R.string.vehicle_lights),
            value =
                when (state.lights) {
                    true -> stringResource(R.string.vehicle_on)
                    false -> stringResource(R.string.vehicle_off)
                    null -> "—"
                },
        )
        if (state.reverse == true) {
            VehicleRow(
                icon = Lucide.ArrowDown,
                label = stringResource(R.string.vehicle_reverse),
                value = stringResource(R.string.vehicle_engaged),
                warn = true,
            )
        }
    }
}

@Composable
private fun VehicleRow(
    icon: ImageVector,
    label: String,
    value: String,
    warn: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        FemtoIcon(
            imageVector = icon,
            contentDescription = null,
            tint = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.glanceCaption(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.glanceCaption(),
            color = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
