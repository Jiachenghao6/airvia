package com.opus.airvia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** "New version" strip above every tab, shown when Updater finds a newer release. */
@Composable
fun UpdateBanner(
    update: Updater.Update,
    progress: Int?,
    error: String?,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = Airvia.palette
    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.signal)
            .padding(16.dp),
    ) {
        Text(
            "有新版本 ${update.version}",
            style = MaterialTheme.typography.titleMedium,
            color = p.onSignal,
        )
        if (update.notes.isNotBlank()) {
            Text(
                update.notes,
                style = MaterialTheme.typography.bodySmall,
                color = p.onSignal,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = p.onSignal,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                color = p.onSignal,
                trackColor = p.signal,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "正在下载 $progress%",
                style = ReadoutStyle,
                color = p.onSignal,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = p.onSignal,
                        contentColor = p.signal,
                    ),
                ) { Text("更新") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("以后再说", color = p.onSignal) }
            }
        }
    }
}
