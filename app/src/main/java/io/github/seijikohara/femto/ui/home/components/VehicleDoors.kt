// AVIN Avant 4 fork: top view of the car with open doors and tailgate highlighted.
package io.github.seijikohara.femto.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.seijikohara.femto.R
import io.github.seijikohara.femto.data.vehicle.Doors
import io.github.seijikohara.femto.ui.theme.glanceCaption
import kotlin.math.cos
import kotlin.math.sin

private val CAR_LENGTH = 46.dp
private val CAR_WIDTH = 26.dp

// Opening angle of a drawn open door, from the body side.
private const val DOOR_SWING_RAD = 0.6f

// Body outline, as fractions of the glyph size (front at the top).
private const val BODY_WIDTH = 0.58f
private const val BODY_TOP = 0.03f
private const val BODY_BOTTOM = 0.88f
private const val WINDSHIELD = 0.32f
private const val FRONT_DOOR_TOP = 0.36f
private const val REAR_DOOR_TOP = 0.56f
private const val REAR_DOOR_BOTTOM = 0.74f
private const val REAR_WINDOW = 0.78f
private const val TAILGATE_LIFT = 0.08f

/**
 * Doors and tailgate from the [Doors] bit mask: a small top view of the car
 * (front to the right) with the open ones swung out, next to "all closed" or a
 * highlighted "Driver's door open" / "2 doors open" line. The warning lives in
 * the card itself, never in a popup, and shows in motion as well.
 * [driverLeft] places the driver's door on the car's left (left-hand drive).
 */
@Composable
internal fun VehicleDoors(
    doors: Int,
    modifier: Modifier = Modifier,
    driverLeft: Boolean = true,
) {
    val open = doors and Doors.ALL
    val message = doorsOpenMessage(open)
    val alert = message != null
    val content = if (alert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val doorColor = MaterialTheme.colorScheme.error
    val status = message ?: stringResource(R.string.vehicle_doors_closed)
    val description = stringResource(R.string.vehicle_doors) + ": " + status
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = description },
        shape = MaterialTheme.shapes.small,
        color = if (alert) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
        contentColor = content,
    ) {
        val glyph: @Composable () -> Unit = {
            Canvas(Modifier.size(width = CAR_LENGTH, height = CAR_WIDTH)) {
                // Drawn front-up in a CAR_WIDTH x CAR_LENGTH box, turned to lie lengthwise.
                val box = Size(CAR_WIDTH.toPx(), CAR_LENGTH.toPx())
                rotate(degrees = 90f) {
                    translate(left = (size.width - box.width) / 2, top = (size.height - box.height) / 2) {
                        drawCarTopView(box, open = open, driverLeft = driverLeft, outline = content, alert = doorColor)
                    }
                }
            }
        }
        if (alert) {
            // The message gets the card's full width below the car: a door name must not break mid-word.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                glyph()
                Text(
                    text = status,
                    style = MaterialTheme.typography.glanceCaption(),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                glyph()
                Text(
                    text = status,
                    style = MaterialTheme.typography.glanceCaption(),
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

private val SingleDoorMessages =
    listOf(
        Doors.DRIVER to R.string.vehicle_door_open_driver,
        Doors.PASSENGER to R.string.vehicle_door_open_passenger,
        Doors.REAR_LEFT to R.string.vehicle_door_open_rear_left,
        Doors.REAR_RIGHT to R.string.vehicle_door_open_rear_right,
        Doors.TAILGATE to R.string.vehicle_door_open_tailgate,
    )

/** "Driver's door open" for one open door or tailgate, "2 doors open" for more, null when all are closed. */
@Composable
internal fun doorsOpenMessage(doors: Int): String? {
    val open = doors and Doors.ALL
    val count = Integer.bitCount(open)
    return when (count) {
        0 -> null
        1 -> stringResource(SingleDoorMessages.first { (bit, _) -> open and bit != 0 }.second)
        else -> pluralStringResource(R.plurals.vehicle_doors_open_count, count, count)
    }
}

private fun DrawScope.drawCarTopView(
    box: Size,
    open: Int,
    driverLeft: Boolean,
    outline: Color,
    alert: Color,
) {
    val w = box.width
    val h = box.height
    val stroke = 1.5.dp.toPx()
    val doorStroke = 2.5.dp.toPx()
    val bodyW = w * BODY_WIDTH
    val left = (w - bodyW) / 2
    val right = left + bodyW
    val top = h * BODY_TOP
    val bottom = h * BODY_BOTTOM
    val corner = CornerRadius(bodyW * 0.32f, bodyW * 0.32f)

    // Glass house (windshield to rear window) lightly filled, then the body outline.
    val inset = bodyW * 0.14f
    val glass =
        Path().apply {
            moveTo(left + inset, h * WINDSHIELD)
            quadraticTo(w / 2, h * (WINDSHIELD - 0.04f), right - inset, h * WINDSHIELD)
            lineTo(right - inset, h * REAR_WINDOW)
            quadraticTo(w / 2, h * (REAR_WINDOW + 0.03f), left + inset, h * REAR_WINDOW)
            close()
        }
    drawPath(glass, outline.copy(alpha = 0.18f))
    drawPath(glass, outline, style = Stroke(width = stroke * 0.7f))
    val tailgateOpen = open and Doors.TAILGATE != 0
    drawRoundRect(
        color = outline,
        topLeft = Offset(left, top),
        size = Size(bodyW, bottom - top),
        cornerRadius = corner,
        style = Stroke(width = stroke),
    )

    // Door seams on both sides, so closed doors still read as doors.
    listOf(FRONT_DOOR_TOP, REAR_DOOR_TOP, REAR_DOOR_BOTTOM).forEach { y ->
        drawLine(outline, Offset(left, h * y), Offset(left + inset * 0.6f, h * y), stroke * 0.7f)
        drawLine(outline, Offset(right, h * y), Offset(right - inset * 0.6f, h * y), stroke * 0.7f)
    }

    // Open doors: hinged at their front edge, swung outward.
    val (frontLeft, frontRight) = if (driverLeft) Doors.DRIVER to Doors.PASSENGER else Doors.PASSENGER to Doors.DRIVER
    val (rearLeft, rearRight) =
        if (driverLeft) Doors.REAR_LEFT to Doors.REAR_RIGHT else Doors.REAR_RIGHT to Doors.REAR_LEFT

    fun door(
        bit: Int,
        leftSide: Boolean,
        from: Float,
        to: Float,
    ) {
        if (open and bit == 0) return
        val x = if (leftSide) left else right
        val length = h * (to - from)
        val dir = if (leftSide) -1f else 1f
        val hinge = Offset(x, h * from)
        val tip = Offset(x + dir * length * sin(DOOR_SWING_RAD), h * from + length * cos(DOOR_SWING_RAD))
        // Tint the opening in the body side, then the swung-out door.
        drawLine(alert.copy(alpha = 0.35f), hinge, Offset(x, h * to), stroke)
        drawLine(alert, hinge, tip, doorStroke, cap = StrokeCap.Round)
    }
    door(frontLeft, leftSide = true, from = FRONT_DOOR_TOP, to = REAR_DOOR_TOP)
    door(frontRight, leftSide = false, from = FRONT_DOOR_TOP, to = REAR_DOOR_TOP)
    door(rearLeft, leftSide = true, from = REAR_DOOR_TOP, to = REAR_DOOR_BOTTOM)
    door(rearRight, leftSide = false, from = REAR_DOOR_TOP, to = REAR_DOOR_BOTTOM)

    // Open tailgate: the rear end lifted back past the body.
    if (tailgateOpen) {
        val lift = h * TAILGATE_LIFT
        val lid =
            Path().apply {
                moveTo(left + inset * 0.4f, h * REAR_WINDOW + lift * 0.3f)
                lineTo(left + inset * 0.4f, bottom + lift - corner.y * 0.4f)
                quadraticTo(
                    w / 2,
                    bottom + lift + corner.y * 0.2f,
                    right - inset * 0.4f,
                    bottom + lift - corner.y * 0.4f,
                )
                lineTo(right - inset * 0.4f, h * REAR_WINDOW + lift * 0.3f)
            }
        drawPath(lid, alert, style = Stroke(width = doorStroke, cap = StrokeCap.Round))
    }
}
