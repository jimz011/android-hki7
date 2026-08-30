package com.jimz011apps.hki7.data

import java.io.BufferedOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

internal object MjpegFrameHub {
    private val latest = AtomicReference<ByteArray?>(null)
    private val lock = Object()

    fun publish(jpeg: ByteArray) {
        latest.set(jpeg)
        synchronized(lock) { lock.notifyAll() }
    }

    fun awaitFrame(timeoutMs: Long): ByteArray? {
        val current = latest.get()
        if (current != null && timeoutMs <= 0L) return current
        synchronized(lock) {
            if (latest.get() === current) {
                runCatching { lock.wait(timeoutMs.coerceAtLeast(1L)) }
            }
        }
        return latest.get() ?: current
    }

    fun clear() {
        latest.set(null)
    }
}

internal class MjpegHttpServer(
    private val port: Int,
    private val requiredToken: String,
) {
    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private val clients = AtomicInteger(0)

    val isRunning: Boolean get() = running

    /**
     * Binds [port] on [bindAddress] (the tablet's LAN IPv4) so a VPN or guest interface cannot
     * reach the stream. Rejects requests without [requiredToken]. Does not set [running] until
     * the bind succeeds.
     */
    fun start(bindAddress: InetAddress? = null): Result<Unit> {
        if (requiredToken.isBlank()) {
            return Result.failure(IllegalStateException("missing_token"))
        }
        if (running) return Result.success(Unit)
        val socket = runCatching {
            if (bindAddress != null) ServerSocket(port, 4, bindAddress) else ServerSocket(port)
        }.getOrElse { return Result.failure(it) }
        serverSocket = socket
        running = true
        executor.execute {
            while (running) {
                val client = try {
                    socket.accept()
                } catch (_: Exception) {
                    break
                }
                executor.execute { handle(client) }
            }
        }
        return Result.success(Unit)
    }

    fun stop() {
        running = false
        runCatching { serverSocket?.close() }
        serverSocket = null
        executor.shutdownNow()
        MjpegFrameHub.clear()
    }

    private fun handle(socket: Socket) {
        socket.soTimeout = 30_000
        try {
            val input = socket.getInputStream().bufferedReader()
            val request = input.readLine() ?: return
            val target = request.substringAfter(' ', "").substringBefore(' ')
            val path = target.substringBefore('?').lowercase()
            val query = target.substringAfter('?', "")
            val output = BufferedOutputStream(socket.getOutputStream())
            if (!request.startsWith("GET ") || (path != "/" && path != "/camera" && path != "/mjpeg")) {
                writePlain(output, 404, "Not found")
                return
            }
            if (!tokenMatches(queryToken(query))) {
                writePlain(output, 401, "Unauthorized")
                return
            }
            if (clients.incrementAndGet() > 4) {
                clients.decrementAndGet()
                writePlain(output, 503, "Too many viewers")
                return
            }
            try {
                stream(output)
            } finally {
                clients.decrementAndGet()
            }
        } catch (_: Exception) {
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun queryToken(query: String): String =
        query.split('&').firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=')
            val value = part.substringAfter('=', "")
            if (key == "token") value else null
        }.orEmpty()

    private fun tokenMatches(provided: String): Boolean {
        val expected = requiredToken.toByteArray(Charsets.UTF_8)
        val actual = provided.toByteArray(Charsets.UTF_8)
        if (expected.size != actual.size) return false
        return java.security.MessageDigest.isEqual(expected, actual)
    }

    private fun stream(output: BufferedOutputStream) {
        val header = (
            "HTTP/1.1 200 OK\r\n" +
                "Connection: close\r\n" +
                "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
                "Pragma: no-cache\r\n" +
                "Content-Type: multipart/x-mixed-replace; boundary=frame\r\n\r\n"
            ).toByteArray(Charsets.US_ASCII)
        output.write(header)
        output.flush()
        while (running) {
            val jpeg = MjpegFrameHub.awaitFrame(250) ?: continue
            val part = (
                "--frame\r\n" +
                    "Content-Type: image/jpeg\r\n" +
                    "Content-Length: ${jpeg.size}\r\n\r\n"
                ).toByteArray(Charsets.US_ASCII)
            output.write(part)
            output.write(jpeg)
            output.write("\r\n".toByteArray(Charsets.US_ASCII))
            output.flush()
        }
    }

    private fun writePlain(output: BufferedOutputStream, code: Int, message: String) {
        val body = message.toByteArray(Charsets.UTF_8)
        val header = (
            "HTTP/1.1 $code $message\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
            ).toByteArray(Charsets.US_ASCII)
        output.write(header)
        output.write(body)
        output.flush()
    }
}
