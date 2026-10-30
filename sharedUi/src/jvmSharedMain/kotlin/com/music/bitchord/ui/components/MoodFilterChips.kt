package com.music.bitchord.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.HomeShelf
import java.util.Locale

/**
 * Quick mood filter categories for ChordPlay Living Home Screen.
 * Pure text labels without emojis for a clean, professional aesthetic.
 */
enum class HomeMood(
    val label: String,
    val keywords: List<String>,
) {
    ALL("All", emptyList()),
    CHILL("Chill", listOf("chill", "relax", "lo-fi", "lofi", "peaceful", "calm", "acoustic", "ambient", "soft", "jazz", "mellow")),
    ENERGY("Energy", listOf("energy", "upbeat", "dance", "electronic", "hype", "boost", "rock", "hits", "edm", "pop", "power")),
    FOCUS("Focus", listOf("focus", "study", "work", "instrumental", "concentration", "deep", "piano", "classical", "reading")),
    WORKOUT("Workout", listOf("workout", "gym", "fitness", "running", "cardio", "power", "pump", "training", "exercise", "motivation")),
    PARTY("Party", listOf("party", "club", "dance", "celebration", "remix", "weekend", "banger", "festival", "night")),
    SLEEP("Sleep", listOf("sleep", "night", "bedtime", "dream", "meditation", "gentle", "rain", "lullaby", "serene"));

    /**
     * Filters home shelves based on keywords matching shelf titles, subtitles, or items.
     * Guarantees a non-empty result by falling back gracefully.
     */
    fun filterShelves(shelves: List<HomeShelf>): List<HomeShelf> {
        if (this == ALL || keywords.isEmpty()) return shelves

        val filtered = shelves.mapNotNull { shelf ->
            val titleMatches = keywords.any { kw ->
                shelf.title.lowercase(Locale.ROOT).contains(kw) ||
                    shelf.subtitle.lowercase(Locale.ROOT).contains(kw)
            }

            if (titleMatches) {
                shelf
            } else {
                val matchingItems = shelf.items.filter { item ->
                    keywords.any { kw ->
                        item.title.lowercase(Locale.ROOT).contains(kw) ||
                            item.subtitle.lowercase(Locale.ROOT).contains(kw)
                    }
                }
                if (matchingItems.isNotEmpty()) {
                    shelf.copy(items = matchingItems)
                } else {
                    null
                }
            }
        }

        return if (filtered.isNotEmpty()) filtered else shelves
    }
}

/**
 * Horizontally scrollable mood filter chips for quick vibes on the Home screen.
 */
@Composable
fun MoodFilterChips(
    selectedMood: HomeMood,
    onMoodSelect: (HomeMood) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = PAGE_GUTTER, vertical = 6.dp),
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(HomeMood.entries, key = { it.name }) { mood ->
            MoodChip(
                mood = mood,
                isSelected = mood == selectedMood,
                onClick = { onMoodSelect(mood) },
            )
        }
    }
}

@Composable
private fun MoodChip(
    mood: HomeMood,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val isLightPrimary = primaryColor.luminance() > 0.5f

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else surfaceVariant.copy(alpha = 0.55f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "moodChipBackground",
    )

    val selectedContentColor = if (isLightPrimary) Color(0xFF0F080B) else MaterialTheme.colorScheme.onPrimary

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "moodChipContent",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isLightPrimary) Color.Black.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.8f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "moodChipBorder",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = if (isLightPrimary) 4.dp else 8.dp,
                        shape = CircleShape,
                        spotColor = if (isLightPrimary) Color.Black.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.5f),
                        ambientColor = if (isLightPrimary) Color.Black.copy(alpha = 0.12f) else primaryColor.copy(alpha = 0.25f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = mood.label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.2.sp,
        )
    }
}
