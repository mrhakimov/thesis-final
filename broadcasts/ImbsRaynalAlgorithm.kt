package broadcasts

import common.HostName
import common.Message
import common.Utils.Companion.basePath
import common.Utils.Companion.timestampFormat
import udp.Utils
import java.io.File
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.util.*

class ImbsRaynalAlgorithm(
    private var sourcePort: Int,
    var port: Int,
    private var hosts: List<HostName>,
    private val n: Int,
    private val f: Int
) : Thread() {
    private val socket = DatagramSocket(port).apply { reuseAddress = true }
    private val utils: Utils = Utils(socket)
    private var running = false

    private var firstInit: Boolean = true
    private var sentWitness: Boolean = false
    private var delivered: Boolean = false
    private val witnesses = mutableMapOf<HostName, Message>()

    var done = false

    private fun concat(type: MessageType, from: HostName, to: HostName, message: Message): String {
        return "${type}_${from}_${to}_${message}"
    }

    private fun broadcast(message: Message) {
        for (host in hosts) {
            val messageToSend = concat(MessageType.Init, "localhost:$sourcePort", host, message)
            utils.socketSend(host, messageToSend)
        }
    }

    private fun init(from: HostName, message: Message) {
        if (from != "localhost:$sourcePort" || !firstInit) {
            return
        }

        firstInit = false

        if (!sentWitness) {
            sentWitness = true

            for (host in hosts) {
                val messageToSend = concat(MessageType.Witness, "localhost:$port", host, message)
                utils.socketSend(host, messageToSend)
            }
        }
    }

    private fun witness(from: HostName, message: Message) {
        witnesses.putIfAbsent(from, message)

        val messageCnt = mutableMapOf<Message, Int>()
        for ((_, msg) in witnesses) {
            messageCnt.putIfAbsent(msg, 0)
            messageCnt[msg] = messageCnt[msg]!! + 1

            if (msg != "" && messageCnt[msg]!! >= (n - 2 * f) && !sentWitness) {
                sentWitness = true

                for (host in hosts) {
                    val messageToSend = concat(MessageType.Witness, "localhost:$port", host, message)
                    utils.socketSend(host, messageToSend)
                }
            }

            if (msg != "" && messageCnt[msg]!! >= (n - f) && !delivered) {
                delivered = true
                done = true

                FileOutputStream(
                    File("$basePath/imbs_${n}_${f}_${port - sourcePort + 9000}"),
                    true
                ).bufferedWriter().use { writer ->
                    writer.write("${timestampFormat.format(Date())} localhost:${port - sourcePort + 9000} delivered message $msg\n")
                }
            }
        }
    }

    override fun run() {
        running = true

        while (running) {
            val packet = utils.receivePacket()
            val parts = packet.data.split("_")

            when (parts[0]) {
                "Broadcast" -> broadcast(parts[1])
                "Init" -> init(parts[1], parts[3])
                "Witness" -> witness(parts[1], parts[3])
                "End" -> {
                    running = false
                    continue
                }
            }
        }

        utils.socket.close()
        Utils.messageSender.shutdown()
    }

    companion object {
        enum class MessageType {
            Init, Witness
        }
    }
}
