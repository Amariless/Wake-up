package com.fritangui.wakeup.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radios de esquina del rediseño 2026 ("--r-sm/--r-md/--r-lg" de los mockups), consistentes en
 *  8-16dp como pide el brief — más contenidos que el redondeo anterior, que llegaba a 32dp. */
val WakeUpShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(26.dp),
)
