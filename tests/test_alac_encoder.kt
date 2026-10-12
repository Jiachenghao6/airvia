import com.opus.airvia.raop.AlacEncoder

/**
 * JVM self-tests for the RAOP verbatim ALAC encoder (Airvia 1.2.4 fix
 * round 3, issue #2): a stereo frame must be ONE CPE (channel-pair)
 * element with interleaved L/R samples — libshairplay's decoder reads
 * exactly one element per frame, so the old two-SCE framing decoded
 * the right channel as zeros (proven on the local rig).
 *
 * These tests parse the produced bitstream back: header fields, the
 * sample-count flag/count for short frames, interleaving order, the
 * ID_END terminator, and the exact encoded sizes (1412 B full frame).
 */
var failures = 0

fun check(name: String, cond: Boolean) {
    if (cond) println("PASS: $name") else {
        println("FAIL: $name")
        failures++
    }
}

/** MSB-first bit reader over the encoded frame. */
class BitReader(val b: ByteArray) {
    var pos = 0 // bit position
    fun read(n: Int): Long {
        var v = 0L
        for (i in 0 until n) {
            val byte = b[pos / 8].toInt() and 0xFF
            val bit = (byte shr (7 - (pos % 8))) and 1
            v = (v shl 1) or bit.toLong()
            pos++
        }
        return v
    }
}

fun main() {
    // --- Constants ---
    check("FRAME_BYTES is 1412", AlacEncoder.FRAME_BYTES == 1412)
    check("FRAME_SAMPLES is 352", AlacEncoder.FRAME_SAMPLES == 352)

    // --- Full frame: structure + interleaving round-trip ---
    val left = ShortArray(352) { (it * 37 - 6000).toShort() }
    val right = ShortArray(352) { (9000 - it * 53).toShort() }
    left[0] = -32768; left[1] = 32767; right[0] = 32767; right[1] = -32768
    val frame = AlacEncoder.encodeFrame(left, right)
    check("full frame is exactly 1412 bytes", frame.size == 1412)

    val r = BitReader(frame)
    check("element tag is 1 (CPE)", r.read(3) == 1L)
    check("element instance is 0", r.read(4) == 0L)
    check("12 header bits are 0", r.read(12) == 0L)
    check("sample-count flag is 0 for full frame", r.read(1) == 0L)
    check("extra bits are 0", r.read(2) == 0L)
    check("escape bit is 1 (verbatim)", r.read(1) == 1L)
    var interleaveOk = true
    for (i in 0 until 352) {
        val l = r.read(16).toInt().toShort()
        val rr = r.read(16).toInt().toShort()
        if (l != left[i] || rr != right[i]) { interleaveOk = false; break }
    }
    check("352 L/R pairs interleaved verbatim, in order", interleaveOk)
    check("ID_END tag (7) follows the samples", r.read(3) == 7L)
    check("bit count consumed rounds up to 1412 bytes", (r.pos + 7) / 8 == 1412)

    // --- Silence encodes and parses identically ---
    val zeros = AlacEncoder.encodeFrame(ShortArray(352), ShortArray(352))
    check("silent full frame is 1412 bytes", zeros.size == 1412)
    val zr = BitReader(zeros)
    zr.read(23)
    var allZero = true
    for (i in 0 until 352 * 2) if (zr.read(16) != 0L) { allZero = false; break }
    check("silent frame samples are all zero", allZero)

    // --- Short frame: count flag + 32-bit count + interleaving ---
    val count = 100
    val sl = ShortArray(count) { (it * 111 - 3000).toShort() }
    val sr = ShortArray(count) { (it * 77 + 1234).toShort() }
    val short = AlacEncoder.encodeFrame(sl, sr, count)
    check("short frame (100) is 408 bytes", short.size == 408)
    val q = BitReader(short)
    check("short: tag is CPE", q.read(3) == 1L)
    check("short: instance is 0", q.read(4) == 0L)
    q.read(12)
    check("short: sample-count flag is 1", q.read(1) == 1L)
    q.read(2)
    check("short: escape bit is 1", q.read(1) == 1L)
    check("short: 32-bit count is 100", q.read(32) == 100L)
    var shortOk = true
    for (i in 0 until count) {
        val l = q.read(16).toInt().toShort()
        val rr = q.read(16).toInt().toShort()
        if (l != sl[i] || rr != sr[i]) { shortOk = false; break }
    }
    check("short: samples interleaved verbatim", shortOk)
    check("short: ID_END follows", q.read(3) == 7L)

    // --- Single-sample frame (lower bound) ---
    val one = AlacEncoder.encodeFrame(shortArrayOf(4242), shortArrayOf(-4242), 1)
    val o = BitReader(one)
    o.read(3); o.read(4); o.read(12)
    check("count=1: flag is 1", o.read(1) == 1L)
    o.read(2); o.read(1)
    check("count=1: count field is 1", o.read(32) == 1L)
    check("count=1: L sample", o.read(16).toInt().toShort() == 4242.toShort())
    check("count=1: R sample", o.read(16).toInt().toShort() == (-4242).toShort())
    check("count=1: ID_END follows", o.read(3) == 7L)

    if (failures > 0) {
        println("RESULT: $failures FAILURES")
        kotlin.system.exitProcess(1)
    }
    println("RESULT: ALL PASS")
}
