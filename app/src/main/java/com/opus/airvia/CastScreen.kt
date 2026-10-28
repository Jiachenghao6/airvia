package com.opus.airvia

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Cast tab, kept to two jobs: pick a speaker, and show the volume the
 * phone's side buttons are setting. Everything else lives on Tune.
 */
@Composable
fun CastScreen(
    castState: CastEngine.State,
    activeSpeakers: List<Speaker>,
    castError: String?,
    sessionStates: Map<String, CastEngine.State>,
    sessionVolumes: Map<String, Int>,
    speakers: List<Speaker>,
    scanning: Boolean,
    onCast: (Speaker) -> Unit,
    onStopSpeaker: (Speaker) -> Unit,
    onStopAll: () -> Unit,
    onRescan: () -> Unit,
    lastSpeaker: Speaker?,
) {
    // The last-used speaker stays listed even before discovery finds it again.
    val listed = if (lastSpeaker != null && speakers.none { it.key == lastSpeaker.key }) {
        listOf(lastSpeaker) + speakers
    } else {
        speakers
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        item {
            if (castState == CastEngine.State.IDLE) {
                Spacer(Modifier.height(8.dp))
            } else {
                VolumeHero(
                    state = castState,
                    activeSpeakers = activeSpeakers,
                    volumes = sessionVolumes,
                    error = castError,
                    onStopAll = onStopAll,
                )
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            ) {
                Text(
                    "Speakers on this Wi-Fi",
                    style = MaterialTheme.typography.titleMedium,
                    color = Airvia.palette.legend,
                    modifier = Modifier.weight(1f),
                )
                if (scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp,
                        color = Airvia.palette.legend,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = onRescan) { Text("Search again") }
            }
        }

        if (listed.isEmpty()) {
            item {
                Text(
                    if (scanning) {
                        "Searching… Turn the speaker on and keep this phone on the same Wi-Fi."
                    } else {
                        "No speakers found. Check the speaker is on the same Wi-Fi, then search again."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Airvia.palette.legend,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        } else {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Airvia.palette.panel),
                ) {
                    listed.forEachIndexed { i, sp ->
                        if (i > 0) {
                            HorizontalDivider(
                                color = Airvia.palette.plate,
                                thickness = 1.dp,
                                modifier = Modifier.padding(start = 44.dp),
                            )
                        }
                        val st = sessionStates[sp.key]
                        SpeakerRow(
                            speaker = sp,
                            state = st,
                            volumePct = sessionVolumes[sp.key],
                            lastUsed = lastSpeaker?.key == sp.key && st == null,
                            onCast = { onCast(sp) },
                            onStop = { onStopSpeaker(sp) },
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** While casting: the speaker(s), and the volume as one big readout. */
@Composable
private fun VolumeHero(
    state: CastEngine.State,
    activeSpeakers: List<Speaker>,
    volumes: Map<String, Int>,
    error: String?,
    onStopAll: () -> Unit,
) {
    val p = Airvia.palette
    val isError = state == CastEngine.State.ERROR
    Column(Modifier.padding(top = 8.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Led(state)
            Spacer(Modifier.width(10.dp))
            Text(
                when (state) {
                    CastEngine.State.CONNECTING -> "Connecting to ${activeSpeakers.names()}"
                    CastEngine.State.ERROR -> "Couldn't connect"
                    else -> activeSpeakers.names()
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isError) {
            Text(
                error ?: "The speaker didn't answer. Check it's on, then try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.fault,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            val vol = activeSpeakers.firstNotNullOfOrNull { volumes[it.key] }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    vol?.toString() ?: "–",
                    fontFamily = ReadoutFamily,
                    fontSize = 112.sp,
                    lineHeight = 112.sp,
                    color = if (state == CastEngine.State.STREAMING) p.ink else p.legend,
                )
                Text(
                    "%",
                    fontFamily = ReadoutFamily,
                    fontSize = 28.sp,
                    color = p.legend,
                    modifier = Modifier.padding(start = 4.dp, bottom = 20.dp),
                )
            }
            Text(
                "Use the phone's volume buttons — they work from the lock screen too.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.legend,
            )
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onStopAll) {
            Text(if (isError) "Dismiss" else if (activeSpeakers.size > 1) "Stop all" else "Stop")
        }
    }
}

@Composable
private fun SpeakerRow(
    speaker: Speaker,
    state: CastEngine.State?,
    volumePct: Int?,
    lastUsed: Boolean,
    onCast: () -> Unit,
    onStop: () -> Unit,
) {
    val p = Airvia.palette
    val live = state == CastEngine.State.STREAMING || state == CastEngine.State.CONNECTING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (live) onStop() else onCast() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(28.dp)) { Led(state) }
        Column(Modifier.weight(1f)) {
            Text(
                speaker.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (live) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = when {
                lastUsed -> "Last used"
                speaker.kind == SpeakerKind.DLNA -> "DLNA / Sonos"
                speaker.kind == SpeakerKind.CHROMECAST -> "Chromecast"
                else -> "AirPlay"
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = p.legend)
        }
        if (state == CastEngine.State.STREAMING && volumePct != null) {
            Text("$volumePct%", style = ReadoutStyle, color = p.legend)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            if (live) "Stop" else "Connect",
            style = MaterialTheme.typography.labelLarge,
            color = if (live) p.legend else p.ink,
        )
    }
}

/** Status lamp: green steady while streaming, pulsing while connecting. */
@Composable
private fun Led(state: CastEngine.State?) {
    val p = Airvia.palette
    val color: Color = when (state) {
        CastEngine.State.STREAMING, CastEngine.State.CONNECTING -> p.led
        CastEngine.State.ERROR -> p.fault
        else -> p.ledOff
    }
    val a = if (state == CastEngine.State.CONNECTING) {
        val t = rememberInfiniteTransition(label = "led")
        val v by t.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "ledPulse",
        )
        v
    } else {
        1f
    }
    Box(
        Modifier
            .size(10.dp)
            .alpha(a)
            .clip(CircleShape)
            .background(color),
    )
}

private fun List<Speaker>.names() = joinToString(", ") { it.name }
