import com.opus.airvia.raop.RaopConnection

/**
 * JVM self-tests for the Kodi/libshairplay RAOP corrections (Airvia
 * 1.2.4 fix round 2, issue #2): SETUP control_port parsing, the sync
 * packet's type byte / first-packet form, and the retransmit reply's
 * 0x56 wrapper.
 *
 * Background, from reading libshairplay's source: the receiver ignores
 * sync contents but learns the return address for its retransmit
 * requests from any control packet — so sync must go to the receiver's
 * CONTROL port from the socket that listens for retransmit requests,
 * with payload type 0x54 (0xD4 with marker), first packet in the 0x90
 * form; and a retransmit reply must be a 4-byte header (0x80, 0xD6,
 * original seq) + the original packet verbatim — a bare verbatim
 * resend is discarded.
 */
var failures = 0

fun check(name: String, cond: Boolean) {
    if (cond) println("PASS: $name") else {
        println("FAIL: $name")
        failures++
    }
}

fun u32(b: ByteArray, off: Int): Long {
    var v = 0L
    for (i in 0 until 4) v = (v shl 8) or (b[off + i].toLong() and 0xFF)
    return v
}

fun u64(b: ByteArray, off: Int): Long {
    var v = 0L
    for (i in 0 until 8) v = (v shl 8) or (b[off + i].toLong() and 0xFF)
    return v
}

fun main() {
    // --- SETUP Transport parsing: control_port is stored ---
    val conn = RaopConnection("192.168.123.100", 36666, "192.168.123.126", true)
    conn.parseTransport(
        "RTP/AVP/UDP;unicast;interleaved=0-1;mode=record;" +
            "server_port=6000;control_port=6001;timing_port=6002",
    )
    check("transport: control_port parsed", conn.controlPort == 6001)

    val connNoCtl = RaopConnection("192.168.123.100", 36666, "192.168.123.126", true)
    connNoCtl.parseTransport("RTP/AVP/UDP;unicast;server_port=6000;timing_port=6002")
    check("transport: missing control_port -> 0", connNoCtl.controlPort == 0)

    // --- Sync packet: first form ---
    val rtpTs = 0x01020304
    val ntp = (0x83AA7E80L shl 32) or 0x12345678L
    val first = RaopConnection.buildSyncPacket(true, rtpTs, ntp)
    check("sync first: 20 bytes", first.size == 20)
    check("sync first: byte0 is 0x90 (special first form)", first[0] == 0x90.toByte())
    check("sync first: byte1 is 0xD4 (type 0x54 + marker)", first[1] == 0xD4.toByte())
    check("sync first: seq field is 7", first[2] == 0.toByte() && first[3] == 7.toByte())
    check("sync first: rtptime at offset 4", u32(first, 4) == rtpTs.toLong())
    check("sync first: NTP at offset 8", u64(first, 8) == ntp)
    check("sync first: rtptime repeated at offset 16", u32(first, 16) == rtpTs.toLong())

    // --- Sync packet: steady-state form ---
    val next = RaopConnection.buildSyncPacket(false, rtpTs, ntp)
    check("sync next: byte0 is 0x80", next[0] == 0x80.toByte())
    check("sync next: byte1 is 0xD4 (not 0xD6)", next[1] == 0xD4.toByte())
    check(
        "sync next: payload identical to first apart from byte0",
        next.copyOfRange(1, 20).contentEquals(first.copyOfRange(1, 20)),
    )

    // --- Retransmit reply: 0x56 wrapper + original packet verbatim ---
    val orig = ByteArray(12 + 1415) { (it * 7 % 251).toByte() }
    orig[0] = 0x80.toByte()
    orig[1] = 0x60.toByte() // an audio packet: PT 96
    val resend = RaopConnection.buildResendPacket(0xBEEF, orig)
    check("resend: size = 4 + original", resend.size == 4 + orig.size)
    check("resend: byte0 is 0x80", resend[0] == 0x80.toByte())
    check("resend: byte1 is 0xD6 (type 0x56 + marker)", resend[1] == 0xD6.toByte())
    check(
        "resend: header carries original seq",
        resend[2] == 0xBE.toByte() && resend[3] == 0xEF.toByte(),
    )
    check(
        "resend: original packet verbatim after header",
        resend.copyOfRange(4, resend.size).contentEquals(orig),
    )
    val resend0 = RaopConnection.buildResendPacket(0, orig)
    check(
        "resend: seq 0 header",
        resend0[2] == 0.toByte() && resend0[3] == 0.toByte(),
    )

    if (failures > 0) {
        println("RESULT: $failures FAILURES")
        kotlin.system.exitProcess(1)
    }
    println("RESULT: ALL PASS")
}
