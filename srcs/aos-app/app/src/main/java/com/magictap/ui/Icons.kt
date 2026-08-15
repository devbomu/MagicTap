package com.magictap.ui

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.Circle
import compose.icons.feathericons.Copy
import compose.icons.feathericons.Download
import compose.icons.feathericons.MoreVertical
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Power
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Settings
import compose.icons.feathericons.Upload
import compose.icons.feathericons.Wifi
import compose.icons.feathericons.X
import compose.icons.feathericons.Zap

/**
 * The app's icon set — Feather (feathericons.com) via the compose-icons library, chosen
 * over the stock Material icons for a cleaner, more distinctive line look. Referencing
 * every icon through here keeps the set consistent and swappable from one place.
 */
object AppIcons {
    val Power: ImageVector get() = FeatherIcons.Power
    val Add: ImageVector get() = FeatherIcons.Plus
    val Settings: ImageVector get() = FeatherIcons.Settings
    val Close: ImageVector get() = FeatherIcons.X
    val Check: ImageVector get() = FeatherIcons.Check
    val More: ImageVector get() = FeatherIcons.MoreVertical
    val Download: ImageVector get() = FeatherIcons.Download
    val Upload: ImageVector get() = FeatherIcons.Upload
    val Wifi: ImageVector get() = FeatherIcons.Wifi
    val Wake: ImageVector get() = FeatherIcons.Zap
    val Copy: ImageVector get() = FeatherIcons.Copy
    val Refresh: ImageVector get() = FeatherIcons.RefreshCw
    val Success: ImageVector get() = FeatherIcons.CheckCircle
    val Error: ImageVector get() = FeatherIcons.AlertCircle
    val Unchecked: ImageVector get() = FeatherIcons.Circle
    val Back: ImageVector get() = FeatherIcons.ArrowLeft
    val ChevronRight: ImageVector get() = FeatherIcons.ChevronRight
}
