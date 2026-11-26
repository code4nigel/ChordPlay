package com.music.bitchord.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings

/** A unique queue result, so repeating the same action restarts its lifetime. */
data class QueueActionNotice(
    val id: Int,
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/** Brief queue confirmation shown immediately above the mini player. */
@Composable
fun QueueActionNoticeHost(
    notice: QueueActionNotice?,
    modifier: Modifier = Modifier,
) {
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = notice,
        transitionSpec = {
            if (reduceAnimation) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn() + slideInVertically { height -> height / 2 }) togetherWith
                    (fadeOut() + slideOutVertically { height -> height / 2 })
            }
        },
        contentKey = { it?.id },
        label = "queueActionNotice",
        modifier = modifier.fillMaxWidth(),
    ) { current ->
        if (current != null) {
            Surface(
                color = Color(0xFF282828),
                contentColor = Color.White,
                shape = RoundedCornerShape(5.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = if (current.actionLabel != null) 8.dp else 20.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                    )
                    if (current.actionLabel != null && current.onAction != null) {
                        TextButton(onClick = current.onAction) {
                            Text(
                                text = current.actionLabel,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
