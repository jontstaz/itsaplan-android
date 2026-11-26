package io.github.jontstaz.itsaplan.ui.theme

import androidx.compose.ui.graphics.Color

// Brand & Primary Palette — Precision Electric Indigo + Deep Slate
val Indigo400 = Color(0xFF818CF8)
val Indigo500 = Color(0xFF6366F1)
val Indigo600 = Color(0xFF4F46E5)
val Indigo700 = Color(0xFF4338CA)
val Indigo900 = Color(0xFF1E1B4B)
val Indigo100 = Color(0xFFE0E7FF)
val Indigo50 = Color(0xFFEEF2FF)

// Neutral Dark Surfaces (Obsidian Slate)
val Slate950 = Color(0xFF0B0E14)
val Slate900 = Color(0xFF111622)
val Slate850 = Color(0xFF161C2C)
val Slate800 = Color(0xFF1E263A)
val Slate700 = Color(0xFF2B354E)
val Slate600 = Color(0xFF475569)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)

// Neutral Light Surfaces (Crisp Alabaster)
val AlabasterBg = Color(0xFFF8FAFC)
val AlabasterSurface = Color(0xFFFFFFFF)
val AlabasterContainer = Color(0xFFF1F5F9)
val AlabasterContainerHigh = Color(0xFFE2E8F0)
val InkPrimary = Color(0xFF0F172A)
// Outline tokens that clear the 3:1 non-text contrast threshold on their scheme's surface.
val SlateOutline = Color(0xFF5A6B85)
val InkOutline = Color(0xFF7C8CA1)
val InkSecondary = Color(0xFF475569)

// Semantic Status & Priority Tokens
val Emerald500 = Color(0xFF10B981)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald900 = Color(0xFF064E3B)

val Amber500 = Color(0xFFF59E0B)
val Amber100 = Color(0xFFFEF3C7)
val Amber900 = Color(0xFF78350F)

val Coral500 = Color(0xFFEF4444)
val Coral100 = Color(0xFFFEE2E2)
val Coral900 = Color(0xFF7F1D1D)

val Sky500 = Color(0xFF0EA5E9)
val Violet500 = Color(0xFF8B5CF6)
val Rose500 = Color(0xFFF43F5E)
val Zinc500 = Color(0xFF71717A)

// Priority Helpers
val PriorityUrgentColor = Color(0xFFEF4444)
val PriorityHighColor = Color(0xFFF97316)
val PriorityMediumColor = Color(0xFFEAB308)
val PriorityLowColor = Color(0xFF3B82F6)
val PriorityNoneColor = Color(0xFF94A3B8)

// State Type Helpers
val StateBacklogColor = Color(0xFF94A3B8)
val StateUnstartedColor = Color(0xFF64748B)
val StateStartedColor = Color(0xFFF59E0B)
val StateCompletedColor = Color(0xFF10B981)
val StateCanceledColor = Color(0xFFEF4444)

fun parseHexColor(hex: String?, fallback: Color = Indigo500): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val colorInt = when (cleaned.length) {
            6 -> (0xFF000000 or cleaned.toLong(16)).toInt()
            8 -> cleaned.toLong(16).toInt()
            else -> return fallback
        }
        Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}
