package udp

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class SendClient {
    private val socket: DatagramSocket = DatagramSocket()
    private val address: InetAddress =
        InetAddress.getByName("localhost")

    fun send(msg: String, port: Int) {
        val buf = msg.toByteArray()

        val packet = DatagramPacket(buf, buf.size, address, port)
        socket.send(packet)
    }

    fun close() {
        socket.close()
    }
}
