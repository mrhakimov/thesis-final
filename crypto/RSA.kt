package crypto

import common.Utils
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.*
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class RSA {
    companion object {
        fun sign(secretMessage: String, privateKey: PrivateKey): ByteArray {
            val signature: Signature = Signature.getInstance("SHA1withRSA")
            signature.initSign(privateKey, SecureRandom())
            val message: ByteArray = secretMessage.toByteArray()
            signature.update(message)
            return signature.sign()
        }

        fun verify(secretMessage: String, publicKey: PublicKey, sigBytes: ByteArray): Boolean{
            val message: ByteArray = secretMessage.toByteArray()
            val signature1 = Signature.getInstance("SHA1withRSA")
            signature1.initVerify(publicKey)
            signature1.update(message)
            return signature1.verify(sigBytes)
        }

        fun generateKeys(): KeyPair {
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(2048)
            val pair = generator.generateKeyPair()

            val privateKey = pair.private
            val publicKey = pair.public

            FileOutputStream("${Utils.keysPath}/keys/public.key").use { fos -> fos.write(publicKey.encoded) }
            FileOutputStream("${Utils.keysPath}/keys/private.key").use { fos -> fos.write(privateKey.encoded) }

            return KeyPair(publicKey, privateKey)
        }

        fun readKeys(): KeyPair {
            val publicKeyFile = File("${Utils.keysPath}/keys/public.key")
            val publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath())

            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(publicKeyBytes)
            val publicKey = keyFactory.generatePublic(publicKeySpec)

            val privateKeyFile = File("${Utils.keysPath}/keys/private.key")
            val privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath())

            val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val privateKey = keyFactory.generatePrivate(privateKeySpec)

            return KeyPair(publicKey, privateKey)
        }
    }
}
