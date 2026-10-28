package com.opus.airvia

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-update from the fork's GitHub releases.
 *
 * Releases are tagged v1.2.4-fork.N and carry one asset, Airvia.apk
 * (scripts/release.sh). On launch the app asks the GitHub API for the
 * latest release; if its N is higher than ours, the UI offers to update.
 * The APK is streamed into a PackageInstaller session, and Android shows
 * its own confirm screen — the update only installs if the APK is signed
 * with the same key as the installed app.
 */
object Updater {
    private const val LATEST =
        "https://api.github.com/repos/Jiachenghao6/airvia/releases/latest"
    private const val ASSET = "Airvia.apk"

    data class Update(val version: String, val fork: Int, val notes: String, val apkUrl: String)

    /** "1.2.4-fork.3" -> 3; anything else -> 0. */
    fun forkNumber(version: String): Int =
        Regex("""fork\.(\d+)""").find(version)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    fun currentVersion(ctx: Context): String =
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }

    /** Blocking network call: run off the main thread. Null when up to date or unreachable. */
    fun check(ctx: Context): Update? {
        val current = forkNumber(currentVersion(ctx))
        val conn = URL(LATEST).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (conn.responseCode != 200) {
                LogBus.log("[update] GitHub answered ${conn.responseCode}")
                return null
            }
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val version = json.optString("tag_name").removePrefix("v")
            val latest = forkNumber(version)
            val assets = json.optJSONArray("assets")
            var url: String? = null
            for (i in 0 until (assets?.length() ?: 0)) {
                val a = assets!!.getJSONObject(i)
                if (a.optString("name") == ASSET) url = a.optString("browser_download_url")
            }
            LogBus.log("[update] installed fork.$current, latest $version")
            if (latest <= current || url == null) return null
            val notes = json.optString("body")
                .lineSequence()
                .map { it.trim().removePrefix("- ") }
                .filter { it.isNotEmpty() && !it.startsWith("SHA-256") }
                .take(3)
                .joinToString("\n")
            return Update(version, latest, notes, url)
        } finally {
            conn.disconnect()
        }
    }

    /** Android needs a one-time "install unknown apps" grant for Airvia. */
    fun canInstall(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /**
     * Blocking: download the APK (reporting 0..100) and hand it to the
     * system installer, which then asks the user to confirm.
     */
    fun downloadAndInstall(ctx: Context, update: Update, onProgress: (Int) -> Unit) {
        val file = File(ctx.cacheDir, ASSET)
        val conn = URL(update.apkUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 20000
        try {
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done * 100 / total).toInt())
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        LogBus.log("[update] downloaded ${update.version} (${file.length()} bytes)")

        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(ctx.packageName)
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite(ASSET, 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val pending = PendingIntent.getBroadcast(
                ctx,
                id,
                Intent(ctx, UpdateReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pending.intentSender)
        }
        file.delete()
    }
}

/** Relays the installer's result: opens its confirm screen, logs failures. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> LogBus.log("[update] installed")
            else -> LogBus.log(
                "[update] install failed ($status): " +
                    intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
            )
        }
    }
}
