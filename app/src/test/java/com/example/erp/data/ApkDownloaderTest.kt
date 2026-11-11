package com.example.erp.data

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
/**
 * The download's safety decisions, pinned.
 *
 * The network stream itself needs a device and a published release; everything
 * that decides whether bytes may be trusted is pure and lives below.
 */
class ApkDownloaderTest {

    private val releaseSha256 =
        "a198025f107815c7a65143f8f5dc3d525c814be9c078be9b16e6d6f11d45bf12"

    @Test
    fun `sha256 matches the known vector`() {
        // The canonical vector: SHA-256("abc").
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc".toByteArray())
        )
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256Hex(ByteArray(0))
        )
    }

    @Test
    fun `bytes above 0x7f are not sign-extended`() {
        // A naive Byte.toString(16) turns 0x80 into "ffffff80"; the digest must
        // stay two hex digits per byte.
        assertEquals("80ff00", byteArrayOf(0x80.toByte(), 0xFF.toByte(), 0x00).toHexString())
    }

    @Test
    fun `a sha256 digest is read only when it is well formed`() {
        assertEquals(releaseSha256, parseSha256Digest("sha256:$releaseSha256"))
        assertEquals(
            "the algorithm prefix is case insensitive",
            releaseSha256,
            parseSha256Digest("SHA256:${releaseSha256.uppercase()}")
        )
        assertEquals(releaseSha256, parseSha256Digest("  sha256:$releaseSha256  "))

        assertNull(parseSha256Digest(null))
        assertNull(parseSha256Digest(""))
        assertNull(parseSha256Digest("sha256:"))
        assertNull("a truncated hash proves nothing", parseSha256Digest("sha256:${releaseSha256.dropLast(1)}"))
        assertNull("another algorithm cannot be checked here", parseSha256Digest("md5:$releaseSha256"))
        assertNull("not hex", parseSha256Digest("sha256:${"z".repeat(64)}"))
        assertNull(parseSha256Digest(releaseSha256))
    }

    @Test
    fun `the download file name cannot leave the updates directory`() {
        assertEquals("Dolita-1.31.0.apk", updateApkFileName("1.31.0"))
        assertEquals("Dolita-v1.31.0.apk", updateApkFileName("v1.31.0"))

        val hostile = updateApkFileName("../../etc/passwd")
        assertFalse(hostile.contains("/"))
        assertFalse(hostile.contains("\\"))
        assertTrue(hostile.endsWith(".apk"))

        assertEquals("Dolita-update.apk", updateApkFileName(""))
        assertEquals("Dolita-update.apk", updateApkFileName("   "))
    }

    @Test
    fun `progress is a percentage only when the total is known`() {
        assertNull(downloadProgressPercent(0L, 0L))
        assertNull("an unknown total keeps the bar indeterminate", downloadProgressPercent(512L, -1L))
        assertNull(downloadProgressPercent(-1L, 100L))

        assertEquals(0, downloadProgressPercent(0L, 100L))
        assertEquals(50, downloadProgressPercent(50L, 100L))
        assertEquals(100, downloadProgressPercent(100L, 100L))
        assertEquals("a server that lies about size never exceeds 100", 100, downloadProgressPercent(200L, 100L))
    }

    @Test
    fun `pending files are cleared`() {
        val directory = Files.createTempDirectory("dolita-updates").toFile()
        try {
            File(directory, "Dolita-1.31.0.apk").writeBytes(ByteArray(16))
            File(directory, "Dolita-1.31.0.abc.part").writeBytes(ByteArray(16))

            ApkDownloader(directory).clearPending()

            assertTrue("the directory stays usable", directory.isDirectory)
            assertTrue(
                "a downloaded APK must not survive the cleanup",
                directory.listFiles().isNullOrEmpty()
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `a rejected download leaves nothing installable`() = runBlocking {        val payload = ByteArray(300_000) { index -> (index % 251).toByte() }
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/apk") { exchange ->
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        server.start()

        val directory = Files.createTempDirectory("dolita-updates").toFile()
        try {
            val downloader = ApkDownloader(directory)
            val url = "http://127.0.0.1:${server.address.port}/apk"
            val expected = sha256Hex(payload)

            val success = downloader.download(url, expected, "Dolita-1.31.0.apk")
            assertTrue(success is ApkDownloadResult.Success)
            val file = (success as ApkDownloadResult.Success).file
            assertEquals("Dolita-1.31.0.apk", file.name)
            assertEquals("the bytes on disk are the bytes served", expected, sha256Hex(file.readBytes()))

            val tampered = downloader.download(url, "0".repeat(64), "Dolita-1.32.0.apk")
            assertTrue(tampered is ApkDownloadResult.Failure)
            assertTrue(
                "a failed verification must not leave an installable file",
                directory.listFiles().isNullOrEmpty()
            )
        } finally {
            server.stop(0)
            directory.deleteRecursively()
        }
    }
}
