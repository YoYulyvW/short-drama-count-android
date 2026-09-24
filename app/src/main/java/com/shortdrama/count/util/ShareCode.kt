package com.shortdrama.count.util

import com.shortdrama.count.model.ShareItem
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class ShareCodeException(val kind: Kind, message: String = kind.name) : Exception(message) {
    enum class Kind { INVALID_FORMAT, BAD_KEY, INVALID_VERSION }
}

object ShareCode {
    private val platformIDs = mapOf(
        "橙子建站" to 1, "懂车帝" to 2, "易车" to 3, "车主之家" to 4, "瓜子" to 5
    )
    private val idToPlatform = platformIDs.entries.associate { (k, v) -> v.toByte() to k }

    // ---------- pack / unpack ----------
    fun pack(titleOrder: List<Pair<String, Boolean>>,
             groups: Map<String, Map<String, Int>>): ByteArray {
        val buf = ByteArrayOutputStream()
        buf.write(2)
        buf.write(minOf(titleOrder.size, 255))
        for ((title, isFast) in titleOrder.take(255)) {
            val rd = groups["$title|$isFast"] ?: emptyMap()
            buf.write(if (isFast) 1 else 0)
            val tb = title.toByteArray(Charsets.UTF_8)
            buf.write(minOf(tb.size, 255))
            buf.write(tb, 0, minOf(tb.size, 255))
            buf.write(minOf(rd.size, 255))
            for ((pName, cnt) in rd) {
                val pid = platformIDs[pName]
                if (pid != null) {
                    buf.write(pid)
                } else {
                    buf.write(0)
                    val pb = pName.toByteArray(Charsets.UTF_8)
                    buf.write(minOf(pb.size, 255))
                    buf.write(pb, 0, minOf(pb.size, 255))
                }
                buf.write(minOf(maxOf(cnt, 0), 255))
            }
        }
        return buf.toByteArray()
    }

    fun unpack(raw: ByteArray): Pair<List<Pair<String, Boolean>>, Map<String, Map<String, Int>>> {
        if (raw.size < 2 || raw[0].toInt() != 2)
            throw ShareCodeException(ShareCodeException.Kind.INVALID_VERSION)
        var pos = 1
        val n = raw[pos].toInt() and 0xFF; pos++
        val order = mutableListOf<Pair<String, Boolean>>()
        val groups = mutableMapOf<String, Map<String, Int>>()
        for (i in 0 until n) {
            if (pos >= raw.size) throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
            val isFast = raw[pos].toInt() != 0; pos++
            val tlen = raw[pos].toInt() and 0xFF; pos++
            if (pos + tlen > raw.size) throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
            val title = String(raw, pos, tlen, Charsets.UTF_8); pos += tlen
            val pn = raw[pos].toInt() and 0xFF; pos++
            val rd = mutableMapOf<String, Int>()
            for (j in 0 until pn) {
                if (pos >= raw.size) throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
                val pid = raw[pos].toInt() and 0xFF; pos++
                val pName: String
                if (pid == 0) {
                    if (pos >= raw.size) throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
                    val plen = raw[pos].toInt() and 0xFF; pos++
                    if (pos + plen > raw.size) throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
                    pName = String(raw, pos, plen, Charsets.UTF_8); pos += plen
                } else {
                    pName = idToPlatform[pid.toByte()]
                        ?: throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
                }
                if (pos >= raw.size) throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
                val cnt = raw[pos].toInt() and 0xFF; pos++
                rd[pName] = cnt
            }
            order.add(title to isFast)
            groups["$title|$isFast"] = rd
        }
        return order to groups
    }

    // ---------- encrypt / decrypt ----------
    fun encrypt(raw: ByteArray, secret: String = AppConstants.shareSecret): String {
        val comp = compress(raw)
        val rand = SecureRandom()
        val salt = ByteArray(8).also { rand.nextBytes(it) }
        val nonce = ByteArray(8).also { rand.nextBytes(it) }
        val key = pbkdf2(secret, salt, 50000, 32)
        val ks = keystream(key, nonce, comp.size)
        val ct = ByteArray(comp.size) { comp[it].toInt().xor(ks[it].toInt()).toByte() }
        val mac = hmacSHA256(key, salt + nonce + ct).copyOf(12)
        val payload = salt + nonce + ct + mac
        var b64 = Base64.encodeToString(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return AppConstants.sharePrefix + b64
    }

    fun decrypt(text: String, secret: String = AppConstants.shareSecret): ByteArray {
        if (!text.startsWith(AppConstants.sharePrefix))
            throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
        val b64 = text.substring(AppConstants.sharePrefix.length)
        val payload = try {
            Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        } catch (e: Exception) {
            throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
        }
        if (payload.size < 28) throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
        val salt = payload.copyOfRange(0, 8)
        val nonce = payload.copyOfRange(8, 16)
        val macBytes = payload.copyOfRange(payload.size - 12, payload.size)
        val ct = payload.copyOfRange(16, payload.size - 12)
        val key = pbkdf2(secret, salt, 50000, 32)
        val exp = hmacSHA256(key, salt + nonce + ct).copyOf(12)
        if (!exp.contentEquals(macBytes)) throw ShareCodeException(ShareCodeException.Kind.BAD_KEY)
        val ks = keystream(key, nonce, ct.size)
        val comp = ByteArray(ct.size) { ct[it].toInt().xor(ks[it].toInt()).toByte() }
        return decompress(comp)
    }

    fun decryptDCT1(text: String, secret: String = AppConstants.shareSecret): String {
        if (!text.startsWith(AppConstants.encPrefix))
            throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
        val b64 = text.substring(AppConstants.encPrefix.length).trim()
        val payload = try {
            Base64.decode(b64, Base64.DEFAULT)
        } catch (e: Exception) {
            throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
        }
        if (payload.size < 64) throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
        val salt = payload.copyOfRange(0, 16)
        val nonce = payload.copyOfRange(16, 32)
        val macBytes = payload.copyOfRange(payload.size - 32, payload.size)
        val ct = payload.copyOfRange(32, payload.size - 32)
        val key = pbkdf2(secret, salt, 100000, 32)
        val exp = hmacSHA256(key, salt + nonce + ct)
        if (!exp.contentEquals(macBytes)) throw ShareCodeException(ShareCodeException.Kind.BAD_KEY)
        val ks = keystream(key, nonce, ct.size)
        val out = ByteArray(ct.size) { ct[it].toInt().xor(ks[it].toInt()).toByte() }
        return String(out, Charsets.UTF_8)
    }

    // ---------- helpers ----------
    private fun keystream(key: ByteArray, nonce: ByteArray, length: Int): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (out.size() < length) {
            val input = nonce + byteArrayOf(
                (i ushr 24).toByte(), (i ushr 16).toByte(),
                (i ushr 8).toByte(), i.toByte())
            out.write(hmacSHA256(key, input))
            i++
        }
        return out.toByteArray().copyOf(length)
    }

    private fun hmacSHA256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun pbkdf2(pw: String, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(pw.toCharArray(), salt, iterations, keyLength * 8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /** zlib 格式压缩（Java Deflater 默认即 zlib 包装） */
    private fun compress(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream(data.size.coerceAtLeast(64))
        val buf = ByteArray(4096)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            out.write(buf, 0, n)
        }
        deflater.end()
        return out.toByteArray()
    }

    /** 兼容 zlib 与 raw deflate 的解压 */
    private fun decompress(data: ByteArray): ByteArray {
        if (data.size > 6 && (data[0].toInt() and 0xFF) == 0x78) {
            try {
                return inflate(data, 0, data.size)
            } catch (_: Exception) {
                // 尝试剥掉 2 字节头 + 4 字节 adler32 后按 raw deflate
                try {
                    return inflate(data, 2, data.size - 6, nowrap = true)
                } catch (_: Exception) { }
            }
        }
        return try { inflate(data, 0, data.size, nowrap = true) } catch (_: Exception) { ByteArray(0) }
    }

    private fun inflate(data: ByteArray, offset: Int, length: Int, nowrap: Boolean = false): ByteArray {
        val inflater = Inflater(nowrap)
        inflater.setInput(data, offset, length)
        val out = ByteArrayOutputStream(data.size.coerceAtLeast(64) * 3)
        val buf = ByteArray(4096)
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            if (n == 0 && inflater.needsInput()) break
            out.write(buf, 0, n)
        }
        inflater.end()
        return out.toByteArray()
    }
}
