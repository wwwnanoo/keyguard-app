@file:Suppress("unused")

package app.keemobile.kotpass.models

import app.keemobile.kotpass.errors.FormatError
import app.keemobile.kotpass.io.gunzip
import app.keemobile.kotpass.io.gzip
import com.artemchep.keyguard.util.foundation.crypto.createSha256
import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * @property hash Identity of the stored representation, including compression.
 * This is not the SHA-256 of [getContent]; content-based lookup uses BinaryIndex.
 */
sealed class BinaryData(val hash: ByteString) {
    abstract val memoryProtection: Boolean
    abstract val rawContent: ByteArray

    abstract fun getContent(): ByteArray

    class Uncompressed(
        override val memoryProtection: Boolean,
        override val rawContent: ByteArray
    ) : BinaryData(binaryHash(compressed = false, rawContent)) {
        override fun getContent(): ByteArray = rawContent

        fun toCompressed(): Compressed = try {
            Compressed(memoryProtection, getContent().gzip())
        } catch (_: Exception) {
            throw FormatError.FailedCompression("Failed to gzip binary data.")
        }
    }

    class Compressed(
        override val memoryProtection: Boolean,
        override val rawContent: ByteArray
    ) : BinaryData(binaryHash(compressed = true, rawContent)) {
        override fun getContent(): ByteArray = try {
            rawContent.gunzip()
        } catch (error: FormatError) {
            // Preserve specific format errors,
            // instead of masking them behind the generic message below.
            throw error
        } catch (_: Exception) {
            throw FormatError.FailedCompression(
                "Failed to read from compressed binary data stream."
            )
        }
    }
}

private fun binaryHash(compressed: Boolean, content: ByteArray): ByteString =
    createSha256().use { digest ->
        // Tag both variants so raw bytes cannot impersonate a compressed preimage.
        digest.update(byteArrayOf(if (compressed) 1 else 0))
        digest.update(content)
        digest.doFinal().toByteString()
    }
