package udp

import java.net.DatagramPacket
import java.net.InetAddress

class ReceiveResult(var address: InetAddress, var port: Int, var data: String, var length: Int) {
    constructor(packet: DatagramPacket): this(packet.address, packet.port, String(packet.data), packet.length) {
        data = data.substring(0, length)
    }
}
