package broadcasts

import common.HostName
import common.Message
import common.Utils.Companion.basePath
import common.Utils.Companion.timestampFormat
import crypto.Ed25519
import udp.Utils
import java.io.File
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.security.KeyPair
import java.util.*

class PODC21AlgorithmED25519(
    private var sourcePort: Int,
    var port: Int,
    private var hosts: List<HostName>,
    private val n: Int,
    private val f: Int,
    private val pair: KeyPair
) : Thread() {
    private val socket = DatagramSocket(port).apply { reuseAddress = true }
    private val utils: Utils = Utils(socket)
    private var running = false

    private var receivedProposal: Boolean = false
    private var delivered: Boolean = false
    private val votes = mutableMapOf<HostName, Message>()

    var done = false

    private fun concat(type: MessageType, from: HostName, to: HostName, message: Message, sign: ByteArray): String {
        val signToSend: String = Base64.getEncoder().encodeToString(sign)
        return "${type}_${from}_${to}_${message}_${signToSend}"
    }

    private fun broadcast(message: Message) {
        val sign = Ed25519.sign(message, pair.private)

        for (host in hosts) {
            val messageToSend = concat(MessageType.Propose, "localhost:$sourcePort", host, message, sign)
            utils.socketSend(host, messageToSend)
        }
    }

    private fun propose(from: HostName, message: Message) {
        if (from != "localhost:$sourcePort" || receivedProposal) {
            return
        }

        receivedProposal = true
        val sign = Ed25519.sign(message, pair.private)

        for (host in hosts) {
            val messageToSend = concat(MessageType.Vote, "localhost:$port", host, message, sign)
            utils.socketSend(host, messageToSend)
        }
    }

    private fun vote(from: HostName, message: Message, sign: String) {
        val signDecoded = Base64.getDecoder().decode(sign)
        if (Ed25519.verify(message, pair.public, signDecoded)) {
            votes.putIfAbsent(from, message)
        }

        val messageCnt = mutableMapOf<Message, Int>()
        for ((_, msg) in votes) {
            messageCnt.putIfAbsent(msg, 0)
            messageCnt[msg] = messageCnt[msg]!! + 1

            if (msg != "" && messageCnt[msg]!! >= (n - f) && !delivered) {
                for (host in hosts) {
                    val messageToSend = concat(MessageType.Vote, "localhost:$port", host, message, signDecoded)
                    utils.socketSend(host, messageToSend)
                }

                delivered = true
                done = true

                FileOutputStream(
                    File("$basePath/ed25519_${n}_${f}_${port - sourcePort + 9000}"),
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
                "Propose" -> propose(parts[1], parts[3])
                "Vote" -> vote(parts[1], parts[3], parts[4])
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
            Propose, Vote
        }
    }
}
