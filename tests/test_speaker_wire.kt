import com.opus.airvia.Speaker
import com.opus.airvia.SpeakerKind
import com.opus.airvia.SpeakerWire
import com.opus.airvia.dlna.Dlna
import com.opus.airvia.mergeDeviceSpeakers

/**
 * JVM self-tests for the speaker round-trip across the CastService
 * Intent boundary (SpeakerWire.encode -> decode) — the object
 * CastEngine actually receives.
 *
 * Regression for issue #2's second half: the 1.2.4 discovery merge set
 * Speaker.raopPort correctly and the UI showed one merged device, but
 * the RAOP failover still dialed the AirPlay port, because the Intent
 * extras that carry the speaker from the UI into CastService (and back
 * out via speakerFromIntent) had no raopPort/raopEt — the rebuilt
 * Speaker defaulted them to null and runRaop's `raopPort ?: port`
 * silently fell back to the AirPlay port (Kodi: :36667 instead of the
 * RAOP service on :36666).
 */
var failures = 0

fun check(name: String, cond: Boolean) {
    if (cond) println("PASS: $name") else {
        println("FAIL: $name")
        failures++
    }
}

fun svc(name: String, host: String, port: Int, raop: Boolean, et: String? = null): Speaker =
    Speaker(name, host, port).apply {
        isRaopService = raop
        this.et = et
    }

/** The real path: raw NSD entries -> merge -> Intent wire -> engine speaker. */
fun realPath(raw: List<Speaker>): Speaker {
    val merged = mergeDeviceSpeakers(raw)
    check("merge: one device", merged.size == 1)
    return SpeakerWire.decode(SpeakerWire.encode(merged[0]))!!
}

/** Exactly CastEngine.runRaop's target expression. */
fun failoverTarget(sp: Speaker): Int = sp.raopPort ?: sp.port

fun main() {
    // 1) Kodi/LibreELEC through the full real path (AirPlay resolved first).
    val kodi = realPath(
        listOf(
            svc("libreelec", "192.168.123.100", 36667, raop = false),
            svc("libreelec", "192.168.123.100", 36666, raop = true, et = "0,1"),
        ),
    )
    check("kodi: identity survives", kodi.name == "libreelec" && kodi.host == "192.168.123.100")
    check("kodi: AP2 target is the AirPlay port", kodi.port == 36667)
    check("kodi: raopPort survives the Intent", kodi.raopPort == 36666)
    check("kodi: raopEt survives the Intent", kodi.raopEt == "0,1")
    check("kodi: failover target = 36666", failoverTarget(kodi) == 36666)

    // 2) Same device, RAOP service resolved before AirPlay (order seen in
    // tbart's log — must not matter).
    val kodiRev = realPath(
        listOf(
            svc("libreelec", "192.168.123.100", 36666, raop = true, et = "0,1"),
            svc("libreelec", "192.168.123.100", 36667, raop = false),
        ),
    )
    check("kodi reversed: raopPort survives", kodiRev.raopPort == 36666)
    check("kodi reversed: failover target = 36666", failoverTarget(kodiRev) == 36666)

    // 3) HomePod (same port for both services): nothing may change —
    // raopPort stays null and the failover target is the speaker's port.
    val pod = realPath(
        listOf(
            svc("Bedroom", "10.0.0.148", 7000, raop = true, et = "0,3,5"),
            svc("Bedroom", "10.0.0.148", 7000, raop = false),
        ),
    )
    check("homepod: port 7000", pod.port == 7000)
    check("homepod: raopPort still null", pod.raopPort == null)
    check("homepod: et kept", pod.et == "0,3,5")
    check("homepod: failover target = 7000", failoverTarget(pod) == 7000)

    // 4) RAOP-only receiver through the wire.
    val raopOnly = realPath(
        listOf(svc("Express", "10.0.0.50", 5000, raop = true, et = "0,1")),
    )
    check("raop-only: own port, no raopPort", raopOnly.port == 5000 && raopOnly.raopPort == null)

    // 5) DLNA speaker: kind + control endpoints survive unchanged.
    val dlnaSp = Speaker("Sonos", "10.0.0.60", 1400).apply {
        kind = SpeakerKind.DLNA
        dlna = Dlna.DlnaDevice(
            udn = "uuid:RINCON_1",
            name = "Sonos",
            location = "http://10.0.0.60:1400/xml/device_description.xml",
            avTransportUrl = "http://10.0.0.60:1400/MediaRenderer/AVTransport/Control",
            renderingControlUrl = "http://10.0.0.60:1400/MediaRenderer/RenderingControl/Control",
        )
    }
    val dlnaBack = SpeakerWire.decode(SpeakerWire.encode(dlnaSp))!!
    check("dlna: kind survives", dlnaBack.kind == SpeakerKind.DLNA)
    check("dlna: device survives", dlnaBack.dlna?.udn == "uuid:RINCON_1" &&
        dlnaBack.dlna?.avTransportUrl == dlnaSp.dlna!!.avTransportUrl &&
        dlnaBack.dlna?.renderingControlUrl == dlnaSp.dlna!!.renderingControlUrl &&
        dlnaBack.dlna?.location == dlnaSp.dlna!!.location)
    check("dlna: session key unchanged", dlnaBack.key == dlnaSp.key)

    // 6) Chromecast speaker: route id survives unchanged.
    val castSp = Speaker("TV", "", 0).apply {
        kind = SpeakerKind.CHROMECAST
        castRouteId = "route-123"
    }
    val castBack = SpeakerWire.decode(SpeakerWire.encode(castSp))!!
    check("chromecast: kind + route survive", castBack.kind == SpeakerKind.CHROMECAST && castBack.castRouteId == "route-123")

    // 7) A wire map with no host decodes to null (service rejects it).
    check("decode: missing host -> null", SpeakerWire.decode(mapOf("port" to 7000)) == null)

    if (failures > 0) {
        println("RESULT: $failures FAILURES")
        kotlin.system.exitProcess(1)
    }
    println("RESULT: ALL PASS")
}
