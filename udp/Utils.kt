package udp

import common.HostName
// import common.Utils.Companion.timestampFormat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class SendTask(
    private var socket: DatagramSocket,
    private var buf: ByteArray,
    private var address: InetAddress,
    private var port: Int
) : Thread() {
    override fun run() {
        socket.send(DatagramPacket(buf, buf.size, address, port))
    }
}

class Utils(var socket: DatagramSocket) {
    private fun getDelay(): Long {
        val rnd = Random()
        return delay + (rnd.nextGaussian() * (deviation / 2)).toLong()
    }

    fun receivePacket(): ReceiveResult {
        val buf = ByteArray(BUFFER_SIZE)

        val packet = DatagramPacket(buf, buf.size)

        socket.receive(packet)

        return ReceiveResult(packet)
    }

    fun socketSend(to: HostName, message: String) {
        // println("${timestampFormat.format(Date())} " + message.replace("_", " "))
        val hostAndPort = splitHostAndPort(to)
        val address = InetAddress.getByName(hostAndPort.host)

        val buf = message.toByteArray()
        messageSender.schedule(SendTask(socket, buf, address, hostAndPort.port), getDelay(), TimeUnit.MILLISECONDS)
    }

    companion object {
        private const val BUFFER_SIZE = 1024

        var delay: Long = 1000
        var deviation: Long = delay / 10

        val messageSender: ScheduledExecutorService = Executors.newScheduledThreadPool(100)

        data class Address(val host: String, val port: Int)

        private fun splitHostAndPort(host: HostName): Address {
            val parts = host.trim('/').split(":")
            return Address(parts[0], parts[1].toInt())
        }
    }
}
