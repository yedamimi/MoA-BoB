package com.eatda.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// All icons reproduced as Compose ImageVectors matching the prototype's SVG paths.

object EatdaIcons {
    val Home: ImageVector get() = buildIcon("home") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(3f, 11f); lineTo(12f, 4f); lineTo(21f, 11f)
            lineTo(21f, 20f); arcTo(2f, 2f, 0f, false, true, 19f, 22f)
            lineTo(15f, 22f); lineTo(15f, 15f); lineTo(9f, 15f)
            lineTo(9f, 22f); lineTo(5f, 22f)
            arcTo(2f, 2f, 0f, false, true, 3f, 20f); close()
        }
    }

    val Fridge: ImageVector get() = buildIcon("fridge") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            // Outer rect
            moveTo(5f, 2.5f)
            arcTo(2.5f, 2.5f, 0f, false, true, 7.5f, 0f)
            // simplified rect
            moveTo(5f, 2.5f); lineTo(5f, 21.5f)
            arcTo(2.5f, 2.5f, 0f, false, true, 7.5f, 24f)
            lineTo(16.5f, 24f)
            arcTo(2.5f, 2.5f, 0f, false, true, 19f, 21.5f)
            lineTo(19f, 2.5f)
            arcTo(2.5f, 2.5f, 0f, false, true, 16.5f, 0f)
            lineTo(7.5f, 0f)
            arcTo(2.5f, 2.5f, 0f, false, true, 5f, 2.5f)
        }
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(5f, 10f); lineTo(19f, 10f)
            moveTo(9f, 5.5f); lineTo(9f, 7.5f)
            moveTo(9f, 13f); lineTo(9f, 16f)
        }
    }

    val Camera: ImageVector get() = buildIcon("camera") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(5f, 7f); lineTo(7.5f, 7f); lineTo(9f, 5f)
            lineTo(15f, 5f); lineTo(16.5f, 7f); lineTo(19f, 7f)
            arcTo(2f, 2f, 0f, false, true, 21f, 9f)
            lineTo(21f, 18f)
            arcTo(2f, 2f, 0f, false, true, 19f, 20f)
            lineTo(5f, 20f)
            arcTo(2f, 2f, 0f, false, true, 3f, 18f)
            lineTo(3f, 9f)
            arcTo(2f, 2f, 0f, false, true, 5f, 7f)
            close()
        }
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            // Circle in camera
            moveTo(16f, 13f)
            arcTo(4f, 4f, 0f, true, true, 15.999f, 13f)
            close()
        }
    }

    val Bell: ImageVector get() = buildIcon("bell") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(6f, 16f); lineTo(6f, 11f)
            arcTo(6f, 6f, 0f, false, true, 18f, 11f)
            lineTo(18f, 16f); lineTo(19.5f, 18f); lineTo(4.5f, 18f); close()
            moveTo(10f, 21f)
            arcTo(2f, 2f, 0f, false, true, 14f, 21f)
        }
    }

    val Settings: ImageVector get() = buildIcon("settings") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(12f, 9f)
            arcTo(3f, 3f, 0f, true, true, 11.999f, 9f)
            close()
        }
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(19.4f, 15f)
            arcTo(1.7f, 1.7f, 0f, false, false, 19.7f, 16.8f)
            lineTo(19.8f, 16.9f)
            arcTo(2f, 2f, 0f, true, true, 17f, 19.7f)
            lineTo(16.9f, 19.6f)
            arcTo(1.7f, 1.7f, 0f, false, false, 15.1f, 19.3f)
            arcTo(1.7f, 1.7f, 0f, false, false, 14.1f, 20.8f)
            lineTo(14.1f, 21f)
            arcTo(2f, 2f, 0f, true, true, 10.1f, 21f)
            lineTo(10.1f, 20.9f)
            arcTo(1.7f, 1.7f, 0f, false, false, 9f, 19.4f)
            arcTo(1.7f, 1.7f, 0f, false, false, 7.2f, 19.7f)
            lineTo(7.1f, 19.8f)
            arcTo(2f, 2f, 0f, true, true, 4.3f, 17f)
            lineTo(4.4f, 16.9f)
            arcTo(1.7f, 1.7f, 0f, false, false, 4.7f, 15.1f)
            arcTo(1.7f, 1.7f, 0f, false, false, 3.2f, 14.1f)
            lineTo(3f, 14.1f)
            arcTo(2f, 2f, 0f, true, true, 3f, 10.1f)
            lineTo(3.1f, 10.1f)
            arcTo(1.7f, 1.7f, 0f, false, false, 4.6f, 9f)
            arcTo(1.7f, 1.7f, 0f, false, false, 4.3f, 7.2f)
            lineTo(4.2f, 7.1f)
            arcTo(2f, 2f, 0f, true, true, 7f, 4.3f)
            lineTo(7.1f, 7.4f)
            arcTo(1.7f, 1.7f, 0f, false, false, 8.9f, 7.7f)
            lineTo(9f, 7.7f)
            arcTo(1.7f, 1.7f, 0f, false, false, 10f, 6.2f)
            lineTo(10f, 6f)
            arcTo(2f, 2f, 0f, true, true, 14f, 6f)
            lineTo(14f, 6.1f)
            arcTo(1.7f, 1.7f, 0f, false, false, 15f, 7.6f)
            arcTo(1.7f, 1.7f, 0f, false, false, 16.8f, 7.3f)
            lineTo(16.9f, 7.2f)
            arcTo(2f, 2f, 0f, true, true, 19.7f, 10f)
            lineTo(19.6f, 10.1f)
            arcTo(1.7f, 1.7f, 0f, false, false, 19.3f, 11.9f)
            lineTo(19.3f, 12f)
            arcTo(1.7f, 1.7f, 0f, false, false, 20.8f, 13f)
            lineTo(21f, 13f)
            arcTo(2f, 2f, 0f, true, true, 21f, 17f)
            lineTo(20.9f, 17f)
            arcTo(1.7f, 1.7f, 0f, false, false, 19.4f, 18f)
            close()
        }
    }

    val ChevronRight: ImageVector get() = buildIcon("chevR") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f)
        }
    }

    val ChevronLeft: ImageVector get() = buildIcon("chevL") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(15f, 6f); lineTo(9f, 12f); lineTo(15f, 18f)
        }
    }

    val ChevronDown: ImageVector get() = buildIcon("chevD") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
        }
    }

    val Plus: ImageVector get() = buildIcon("plus") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(12f, 5f); lineTo(12f, 19f)
            moveTo(5f, 12f); lineTo(19f, 12f)
        }
    }

    val Close: ImageVector get() = buildIcon("close") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(6f, 6f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
    }

    val Warn: ImageVector get() = buildIcon("warn") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(12f, 3f); lineTo(22f, 21f); lineTo(2f, 21f); close()
            moveTo(12f, 10f); lineTo(12f, 14f)
            moveTo(12f, 17.5f); lineTo(12f, 17.6f)
        }
    }

    val Clock: ImageVector get() = buildIcon("clock") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(21f, 12f)
            arcTo(9f, 9f, 0f, true, true, 20.999f, 12f)
            close()
            moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15f, 14f)
        }
    }

    val Mic: ImageVector get() = buildIcon("mic") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(15f, 3f)
            arcTo(3f, 3f, 0f, false, true, 15f, 15f)
            lineTo(9f, 15f)
            arcTo(3f, 3f, 0f, false, true, 9f, 3f)
            close()
            moveTo(5f, 11f)
            arcTo(7f, 7f, 0f, false, false, 19f, 11f)
            moveTo(12f, 18f); lineTo(12f, 21f)
        }
    }

    val Search: ImageVector get() = buildIcon("search") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(17f, 11f)
            arcTo(6f, 6f, 0f, true, true, 16.999f, 11f)
            close()
            moveTo(16f, 16f); lineTo(21f, 21f)
        }
    }

    val Leaf: ImageVector get() = buildIcon("leaf") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(21f, 3f)
            curveTo(21f, 3f, 12f, 3f, 8f, 7f)
            curveTo(4f, 11f, 4f, 16f, 4f, 16f)
            curveTo(4f, 16f, 9f, 16f, 13f, 12f)
            curveTo(17f, 8f, 21f, 3f, 21f, 3f)
            close()
            moveTo(4f, 21f)
            curveTo(4f, 21f, 8f, 14f, 16f, 6f)
        }
    }

    val Sparkle: ImageVector get() = buildIcon("sparkle") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(12f, 3f); lineTo(12f, 9f)
            moveTo(12f, 15f); lineTo(12f, 21f)
            moveTo(3f, 12f); lineTo(9f, 12f)
            moveTo(15f, 12f); lineTo(21f, 12f)
            moveTo(6f, 6f); lineTo(9f, 9f)
            moveTo(15f, 15f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(15f, 9f)
            moveTo(9f, 15f); lineTo(6f, 18f)
        }
    }

    val Volume: ImageVector get() = buildIcon("vol") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(11f, 5f); lineTo(6f, 9f); lineTo(3f, 9f)
            lineTo(3f, 15f); lineTo(6f, 15f); lineTo(11f, 19f); close()
            moveTo(15f, 9f)
            arcTo(4f, 4f, 0f, false, true, 15f, 15f)
            moveTo(18f, 6f)
            arcTo(8f, 8f, 0f, false, true, 18f, 18f)
        }
    }

    val A11y: ImageVector get() = buildIcon("a11y") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2.5f)
            arcTo(2f, 2f, 0f, true, true, 11.999f, 2.5f)
            close()
        }
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(5f, 9f); lineTo(19f, 9f)
            moveTo(9f, 9f); lineTo(10f, 13f); lineTo(8f, 21f)
            moveTo(15f, 9f); lineTo(14f, 13f); lineTo(16f, 21f)
            moveTo(10f, 13f); lineTo(14f, 13f)
        }
    }

    val Trash: ImageVector get() = buildIcon("trash") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(4f, 7f); lineTo(20f, 7f)
            moveTo(9f, 7f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 7f)
            moveTo(6f, 7f); lineTo(7f, 20f)
            arcTo(2f, 2f, 0f, false, false, 9f, 22f)
            lineTo(15f, 22f)
            arcTo(2f, 2f, 0f, false, false, 17f, 20f)
            lineTo(18f, 7f)
        }
    }

    val Info: ImageVector get() = buildIcon("info") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(21f, 12f)
            arcTo(9f, 9f, 0f, true, true, 20.999f, 12f)
            close()
            moveTo(12f, 8f); lineTo(12f, 8.1f)
            moveTo(12f, 11f); lineTo(12f, 17f)
        }
    }

    val User: ImageVector get() = buildIcon("user") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(16f, 8f)
            arcTo(4f, 4f, 0f, true, true, 15.999f, 8f)
            close()
            moveTo(4f, 21f)
            curveTo(4f, 16.6f, 7.6f, 13f, 12f, 13f)
            curveTo(16.4f, 13f, 20f, 16.6f, 20f, 21f)
        }
    }

    val Package: ImageVector get() = buildIcon("pkg") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(3f, 7f); lineTo(12f, 3f); lineTo(21f, 7f)
            lineTo(12f, 11f); close()
            moveTo(3f, 7f); lineTo(3f, 17f); lineTo(12f, 21f)
            lineTo(21f, 17f); lineTo(21f, 7f)
            moveTo(12f, 11f); lineTo(12f, 21f)
        }
    }

    val Refresh: ImageVector get() = buildIcon("refresh") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(3f, 12f)
            arcTo(9f, 9f, 0f, false, false, 18.5f, 5.7f)
            moveTo(21f, 12f)
            arcTo(9f, 9f, 0f, false, false, 5.5f, 18.3f)
            moveTo(18f, 3f); lineTo(18f, 7f); lineTo(14f, 7f)
            moveTo(6f, 21f); lineTo(6f, 17f); lineTo(10f, 17f)
        }
    }

    val Eye: ImageVector get() = buildIcon("eye") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(2f, 12f)
            curveTo(2f, 12f, 5f, 5f, 12f, 5f)
            curveTo(19f, 5f, 22f, 12f, 22f, 12f)
            curveTo(22f, 12f, 19f, 19f, 12f, 19f)
            curveTo(5f, 19f, 2f, 12f, 2f, 12f)
            close()
        }
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(15f, 12f)
            arcTo(3f, 3f, 0f, true, true, 14.999f, 12f)
            close()
        }
    }

    val FontSize: ImageVector get() = buildIcon("fontSize") {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(4f, 19f); lineTo(10f, 5f); lineTo(16f, 19f)
            moveTo(6f, 14f); lineTo(14f, 14f)
        }
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
            moveTo(16f, 13f); lineTo(19.5f, 5f); lineTo(23f, 13f)
            moveTo(17f, 11f); lineTo(22f, 11f)
        }
    }
}

private fun buildIcon(name: String, block: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()
}

@Composable
fun EatdaIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size),
    )
}
