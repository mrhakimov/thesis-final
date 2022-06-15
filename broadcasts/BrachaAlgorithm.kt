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

class BrachaAlgorithm(
    private var sourcePort: Int,
    var port: Int,
    private var hosts: List<HostName>,
    private val n: Int,
    private val f: Int
) : Thread() {
    private val socket = DatagramSocket(port).apply { reuseAddress = true }
    private val utils: Utils = Utils(socket)
    private var running = false

    private var sentEcho: Boolean = false
    private var sentReady: Boolean = false
    private var delivered: Boolean = false
    private val echos = mutableMapOf<HostName, Message>()
    private val readys = mutableMapOf<HostName, Message>()

    var done = false

    private fun checkReady1() {
        val messageCnt = mutableMapOf<Message, Int>()
        for ((_, msg) in echos) {
            messageCnt.putIfAbsent(msg, 0)
            messageCnt[msg] = messageCnt[msg]!! + 1

            if (msg != "" && messageCnt[msg]!! > (n + f) / 2 && !sentReady) {
                sentReady = true

                for (host in hosts) {
                    val messageToSend = concat(MessageType.Ready, "localhost:$port", host, msg)
                    utils.socketSend(host, messageToSend)
                }
            }
        }
    }

    private fun checkReady2() {
        val messageCnt = mutableMapOf<Message, Int>()
        for ((_, msg) in readys) {
            messageCnt.putIfAbsent(msg, 0)
            messageCnt[msg] = messageCnt[msg]!! + 1

            if (msg != "" && messageCnt[msg]!! > f && !sentReady) {
                sentReady = true

                for (host in hosts) {
                    val messageToSend = concat(MessageType.Ready, "localhost:$port", host, msg)
                    utils.socketSend(host, messageToSend)
                }
            }
        }
    }

    private fun checkDeliver() {
        val messageCnt = mutableMapOf<Message, Int>()
        for ((_, msg) in readys) {
            messageCnt.putIfAbsent(msg, 0)
            messageCnt[msg] = messageCnt[msg]!! + 1

            if (msg != "" && messageCnt[msg]!! > 2 * f && !delivered) {
                delivered = true
                done = true

                FileOutputStream(
                    File("$basePath/brb_${n}_${f}_${port - sourcePort + 9000}"),
                    true
                ).bufferedWriter().use { writer ->
                    writer.write("${timestampFormat.format(Date())} localhost:${port - sourcePort + 9000} delivered message $msg\n")
                }
            }
        }
    }

    private fun doChecks() {
        checkReady1()
        checkReady2()
        checkDeliver()
    }

    private fun concat(type: MessageType, from: HostName, to: HostName, message: Message): String {
        return "${type}_${from}_${to}_${message}"
    }

    private fun broadcast(message: Message) {
        for (host in hosts) {
            val messageToSend = concat(MessageType.Send, "localhost:$sourcePort", host, message)
            utils.socketSend(host, messageToSend)
        }
    }

    private fun send(from: HostName, message: Message) {
        if (from != "localhost:$sourcePort" || sentEcho) {
            return
        }

        sentEcho = true

        for (host in hosts) {
            val messageToSend = concat(MessageType.Echo, "localhost:$port", host, message)
            utils.socketSend(host, messageToSend)
        }

        doChecks()
    }

    private fun echo(from: HostName, message: Message) {
        echos.putIfAbsent(from, message)

        doChecks()
    }

    private fun ready(from: HostName, message: Message) {
        readys.putIfAbsent(from, message)

        doChecks()
    }

    override fun run() {
        running = true

        while (running) {
            val packet = utils.receivePacket()
            val parts = packet.data.split("_")

            when (parts[0]) { // command
                "Broadcast" -> broadcast(parts[1]) // message
                "Send" -> send(parts[1], parts[3]) // from & message
                "Echo" -> echo(parts[1], parts[3])
                "Ready" -> ready(parts[1], parts[3])
                "End" -> running = false
                else -> {
                    running = false
                    println("unexpected command '${parts[0]}'")
                }
            }
        }

        utils.socket.close()
        Utils.messageSender.shutdown()
    }

    companion object {
        enum class MessageType {
            Send, Echo, Ready
        }
    }
}
