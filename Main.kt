import broadcasts.BrachaAlgorithm
import broadcasts.ImbsRaynalAlgorithm
import broadcasts.PODC21AlgorithmED25519
import broadcasts.PODC21AlgorithmRSA
import common.HostName
import common.Utils
import common.Utils.Companion.basePath
import udp.SendClient
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyPair
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.exitProcess


fun bracha(startPort: Int, n: Int, f: Int, msgId: Int) {
    val hosts = mutableListOf<HostName>()
    for (i in startPort until startPort + n) {
        hosts.add("localhost:$i")
    }

    val executor = Executors.newFixedThreadPool(n - f)
    val instances = mutableListOf<BrachaAlgorithm>()

    for (i in startPort until startPort + n - f) {
        try {
            instances.add(BrachaAlgorithm(startPort, i, hosts, n, f))
            executor.execute(instances.last())
        } catch (e: Exception) {
            println("$i: ${e.message}")
        }
    }

    val client = SendClient()

    while (true) {
        val message = Utils.padLeftZeros(msgId.toString(), 4)

        FileOutputStream(
            File("$basePath/brb_${n}_${f}"),
            true
        ).bufferedWriter().use { writer ->
            writer.write("${Utils.timestampFormat.format(Date())} started message $message\n")
        }

        client.send("Broadcast_$message", startPort)
        break
    }

    Thread.sleep(2000) // every broadcast lasts ~3 seconds for message delay equal to 1 second

    for (instance in instances) {
        client.send("End_", instance.port)
    }

    Thread.sleep(1000)

    println("done")

    // releasing resources
    client.close()
    executor.shutdown()
    exitProcess(0)
}

fun imbsRaynal(startPort: Int, n: Int, f: Int, msgId: Int) {
    val executor = Executors.newFixedThreadPool(n - f)

    val hosts = mutableListOf<HostName>()
    for (i in startPort until startPort + n) {
        hosts.add("localhost:$i")
    }

    val instances = mutableListOf<ImbsRaynalAlgorithm>()

    for (i in startPort until startPort + n - f) {
        try {
            instances.add(ImbsRaynalAlgorithm(startPort, i, hosts, n, f))
            executor.execute(instances.last())
        } catch (e: Exception) {
            println("$i ${e.message}")
        }
    }

    val client = SendClient()

    while (true) {
        val message = Utils.padLeftZeros(msgId.toString(), 4)

        FileOutputStream(
            File("$basePath/imbs_${n}_${f}"),
            true
        ).bufferedWriter().use { writer ->
            writer.write("${Utils.timestampFormat.format(Date())} started message $message\n")
        }

        client.send("Broadcast_$message", startPort)
        break
    }

    Thread.sleep(2000) // every broadcast lasts ~2 seconds for message delay equal to 1 second

    for (instance in instances) {
        client.send("End_", instance.port)
    }

    Thread.sleep(1000)

    println("done")

    // releasing resources
    client.close()
    executor.shutdown()
    exitProcess(0)
}

fun podcRSA(startPort: Int, n: Int, f: Int, msgId: Int, pair: KeyPair) {
    val executor = Executors.newFixedThreadPool(n - f)

    val hosts = mutableListOf<HostName>()
    for (i in startPort until startPort + n) {
        hosts.add("localhost:$i")
    }

    val instances = mutableListOf<PODC21AlgorithmRSA>()

    for (i in startPort until startPort + n - f) {
        try {
            instances.add(PODC21AlgorithmRSA(startPort, i, hosts, n, f, pair))
            executor.execute(instances.last())
        } catch (e: Exception) {
            println("$i ${e.message}")
        }
    }

    val client = SendClient()

    while (true) {
        val message = Utils.padLeftZeros(msgId.toString(), 4)

        FileOutputStream(
            File("$basePath/rsa_${n}_${f}"),
            true
        ).bufferedWriter().use { writer ->
            writer.write("${Utils.timestampFormat.format(Date())} started message $message\n")
        }

        client.send("Broadcast_$message", startPort)
        break
    }

    Thread.sleep(2000) // every broadcast lasts ~2 seconds for message delay equal to 1 second

    for (instance in instances) {
        client.send("End_", instance.port)
    }

    Thread.sleep(1000)

    println("done")

    // releasing resources
    client.close()
    executor.shutdown()
    exitProcess(0)
}

fun podcED25519(startPort: Int, n: Int, f: Int, msgId: Int, pair: KeyPair) {
    val executor = Executors.newFixedThreadPool(n - f)

    val hosts = mutableListOf<HostName>()
    for (i in startPort until startPort + n) {
        hosts.add("localhost:$i")
    }

    val instances = mutableListOf<PODC21AlgorithmED25519>()

    for (i in startPort until startPort + n - f) {
        try {
            instances.add(PODC21AlgorithmED25519(startPort, i, hosts, n, f, pair))
            executor.execute(instances.last())
        } catch (e: Exception) {
            println("$i ${e.message}")
        }
    }

    val client = SendClient()

    while (true) {
        val message = Utils.padLeftZeros(msgId.toString(), 4)

        FileOutputStream(
            File("$basePath/ed25519_${n}_${f}"),
            true
        ).bufferedWriter().use { writer ->
            writer.write("${Utils.timestampFormat.format(Date())} started message $message\n")
        }

        client.send("Broadcast_$message", startPort)
        break
    }

    Thread.sleep(2000) // every broadcast lasts ~2 seconds for message delay equal to 1 second

    for (instance in instances) {
        client.send("End_", instance.port)
    }

    Thread.sleep(1000)

    println("done")

    // releasing resources
    client.close()
    executor.shutdown()
    exitProcess(0)
}

fun main(args: Array<String>) {
    val n = args[0].toInt()
    val f = args[1].toInt()
    val delay = args[2].toLong()
    udp.Utils.delay = delay

    basePath = "$basePath/$delay"
    val path = Paths.get(basePath)
    Files.createDirectories(path)

    val msgId = args[3].toInt()
    val algorithm = if (args.size < 5) {
        "brb"
    } else {
        args[4]
    }

    val startPort = 9000

    println("$n $f")

    when (algorithm) {
        "brb" -> bracha(startPort, n, f, msgId)
        "imbs" -> imbsRaynal(startPort, n, f, msgId)
        "rsa" -> {
            val pair: KeyPair = if (args.size < 6) {
                crypto.RSA.readKeys()
            } else {
                crypto.RSA.generateKeys()
            }

            podcRSA(startPort, n, f, msgId, pair)
        }
        "ed" -> {
            val pair: KeyPair = if (args.size < 6) {
                crypto.Ed25519.readKeys()
            } else {
                crypto.Ed25519.generateKeys()
            }

            podcED25519(startPort, n, f, msgId, pair)
        }
    }
}
