package com.opus.airvia

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** One launchable app, for the per-app capture picker. */
data class AppEntry(val packageName: String, val label: String)

/**
 * The Tune tab: 5-band EQ + preamp (applied live to the cast stream),
 * now-playing metadata access, the per-app capture picker, the sleep
 * timer and connect-by-IP — everything the Cast tab leaves out.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TuneScreen(
    eqGains: DoubleArray,
    eqPreamp: Double,
    onEqChange: (DoubleArray, Double) -> Unit,
    nowPlaying: NowPlayingInfo?,
    metaAccessGranted: Boolean,
    onGrantMetaAccess: () -> Unit,
    apps: List<AppEntry>,
    selectedApps: Set<String>,
    onToggleApp: (String, Boolean) -> Unit,
    onCaptureAllApps: () -> Unit,
    sleepMinutes: Int,
    onSleepTimer: (Int) -> Unit,
    hostInput: String,
    portInput: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectIp: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // --- EQ ------------------------------------------------------
        item {
            Text(
                "均衡器",
                style = MaterialTheme.typography.titleMedium,
                color = Airvia.palette.legend,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        for (preset in Eq.PRESETS) {
                            val selected = preset.preampDb == eqPreamp &&
                                preset.gains.indices.all { preset.gains[it] == eqGains[it] }
                            FilterChip(
                                selected = selected,
                                onClick = { onEqChange(preset.gains.copyOf(), preset.preampDb) },
                                label = { Text(preset.name) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    for (band in 0 until DspProcessor.BAND_COUNT) {
                        EqBandRow(
                            label = freqLabel(DspProcessor.BAND_FREQS[band]),
                            db = eqGains[band],
                            onChange = { db ->
                                val g = eqGains.copyOf()
                                g[band] = db
                                onEqChange(g, eqPreamp)
                            },
                        )
                    }
                    EqBandRow(
                        label = "前级",
                        db = eqPreamp,
                        onChange = { db -> onEqChange(eqGains.copyOf(), db) },
                    )
                    Text(
                        "实时作用于所有音箱。在手机端处理声音，AirPlay、DLNA、Chromecast 都生效。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // --- Now playing ----------------------------------------------
        item {
            Text(
                "正在播放",
                style = MaterialTheme.typography.titleMedium,
                color = Airvia.palette.legend,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (!metaAccessGranted) {
                        Text(
                            "音箱可以显示歌名、歌手和封面。需要允许一次通知访问权限，Airvia 才能读到正在播放的内容。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onGrantMetaAccess) {
                            Text("允许通知访问")
                        }
                    } else if (nowPlaying != null) {
                        Text(
                            nowPlaying.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            listOf(nowPlaying.artist, nowPlaying.album)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                                .ifEmpty { "—" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "AirPlay 音箱上显示的就是这些。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "现在没有在播放。在任意 App 里放歌，歌名和封面就会显示到音箱上。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        // --- Sleep timer -----------------------------------------------
        item { SectionTitle("定时停止") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in listOf(0, 15, 30, 60)) {
                            FilterChip(
                                selected = sleepMinutes == m,
                                onClick = { onSleepTimer(m) },
                                label = { Text(if (m == 0) "关" else "$m 分钟") },
                            )
                        }
                    }
                    Text(
                        "连续 5 分钟没有声音也会自动停止投送。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // --- Connect by IP ---------------------------------------------
        item { SectionTitle("按 IP 连接") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "列表里找不到的 AirPlay 音箱，可以在这里手动连接。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = onHostChange,
                            label = { Text("音箱 IP") },
                            placeholder = { Text("192.168.1.20") },
                            singleLine = true,
                            modifier = Modifier.weight(2f),
                        )
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = onPortChange,
                            label = { Text("端口") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onConnectIp, modifier = Modifier.fillMaxWidth()) {
                        Text("连接")
                    }
                }
            }
        }

        // --- Per-app capture -------------------------------------------
        item {
            Text(
                "捕获哪些 App 的声音",
                style = MaterialTheme.typography.titleMedium,
                color = Airvia.palette.legend,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedApps.isEmpty()) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (selectedApps.isEmpty()) {
                                "全部 App"
                            } else {
                                "已选 ${selectedApps.size} 个 App"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (selectedApps.isEmpty()) {
                                "所有 App 的声音都会投出去。"
                            } else {
                                "只投勾选的 App，下次投送时生效。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selectedApps.isNotEmpty()) {
                        TextButton(onClick = onCaptureAllApps) { Text("恢复全部") }
                    }
                }
            }
        }
        items(apps, key = { it.packageName }) { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = app.packageName in selectedApps,
                    onCheckedChange = { checked -> onToggleApp(app.packageName, checked) },
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun EqBandRow(label: String, db: Double, onChange: (Double) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = ReadoutStyle,
            color = Airvia.palette.legend,
            modifier = Modifier.width(56.dp),
        )
        Slider(
            value = db.toFloat(),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = -12f..12f,
            steps = 23,
            colors = SliderDefaults.colors(
                thumbColor = Airvia.palette.ink,
                activeTrackColor = Airvia.palette.ink,
                inactiveTrackColor = Airvia.palette.groove,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            (if (db > 0) "+" else "") + "%.0f dB".format(db),
            style = ReadoutStyle,
            modifier = Modifier.width(52.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = Airvia.palette.legend,
    )
}

private fun freqLabel(hz: Int): String =
    if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz"
