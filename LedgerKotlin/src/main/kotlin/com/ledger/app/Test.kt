import com.ledger.app.utils.implementations.ECCryptoProvider
import com.ledger.app.utils.implementations.RSACryptoProvider
import com.ledger.app.utils.implementations.SHA256HashProvider

fun main() {
    val text = "This is a test"
    val hashProvider = SHA256HashProvider()
    val hash = hashProvider.hash(text)
    val hashedText = hashProvider.toHashString(hash)
    val expectedHash = "c7be1ed902fb8dd4d48997c6452f5d7e509fbcdbe2808b16bcf4edce4c07d14e"
    println("text: $text")
    println("hashedText: $hashedText")
    println("is correct? ${hashedText == expectedHash}")

    println()

    val rsa = RSACryptoProvider()
    val kp_rsa = rsa.generateKeyPair()

    val signature = rsa.sign(text, kp_rsa.private)
    val verified = rsa.verify(text, signature, kp_rsa.public)
    println("RSA verified: $verified")
    val signature2 = rsa.sign(text, kp_rsa.private.encoded)
    val verified2 = rsa.verify(text, signature2, kp_rsa.public.encoded)
    println("RSA verified encoded: $verified2")
    val signature3 = rsa.sign(text, rsa.keyOrSigToString(kp_rsa.private.encoded))
    val verified3 = rsa.verify(text, rsa.keyOrSigToString(signature3), rsa.keyOrSigToString(kp_rsa.public.encoded))
    println("RSA verified string: $verified3")

    println()

    val ec = ECCryptoProvider()
    val ecKeyPair = ec.generateKeyPair()

    val ecSignature1 = ec.sign(text, ecKeyPair.private)
    val ecVerified1 = ec.verify(text, ecSignature1, ecKeyPair.public)
    println("EC verified: $ecVerified1")

    val ecSignature2 = ec.sign(text, ecKeyPair.private.encoded)
    val ecVerified2 = ec.verify(text, ecSignature2, ecKeyPair.public.encoded)
    println("EC verified encoded: $ecVerified2")

    val ecSignature3 = ec.sign(text, ec.keyOrSigToString(ecKeyPair.private.encoded))
    val ecVerified3 = ec.verify(
        text,
        ec.keyOrSigToString(ecSignature3),
        ec.keyOrSigToString(ecKeyPair.public.encoded)
    )
    println("EC verified string: $ecVerified3")
}