import com.ledger.app.utils.hash.HashProvider
import com.ledger.app.utils.signature.SignatureProvider

fun main() {
    val text = "This is a test"
    val hashProvider = HashProvider
    val hash = hashProvider.hash(text, "SHA-256")
    val hashedText = hashProvider.toHashString(hash)
    val expectedHash = "c7be1ed902fb8dd4d48997c6452f5d7e509fbcdbe2808b16bcf4edce4c07d14e"
    println("text: $text")
    println("hashedText: $hashedText")
    println("is correct? ${hashedText == expectedHash}")

    println()

    val rsa = SignatureProvider
    val kp_rsa = rsa.generateKeyPair("RSA")

    val signature = rsa.sign(text, kp_rsa.private, "RSA")
    val verified = rsa.verify(text, signature, kp_rsa.public, "RSA")
    println("RSA verified: $verified")
    val signature2 = rsa.sign(text, kp_rsa.private.encoded, "RSA")
    val verified2 = rsa.verify(text, signature2, kp_rsa.public.encoded, "RSA")
    println("RSA verified encoded: $verified2")
    val signature3 = rsa.sign(text, rsa.keyOrSigToString(kp_rsa.private.encoded), "RSA")
    val verified3 = rsa.verify(text, rsa.keyOrSigToString(signature3), rsa.keyOrSigToString(kp_rsa.public.encoded), "RSA")
    println("RSA verified string: $verified3")

    println()

    val ec = SignatureProvider
    val ecKeyPair = ec.generateKeyPair("ECDSA")

    val ecSignature1 = ec.sign(text, ecKeyPair.private,"ECDSA")
    val ecVerified1 = ec.verify(text, ecSignature1, ecKeyPair.public,"ECDSA")
    println("EC verified: $ecVerified1")

    val ecSignature2 = ec.sign(text, ecKeyPair.private.encoded,"ECDSA")
    val ecVerified2 = ec.verify(text, ecSignature2, ecKeyPair.public.encoded,"ECDSA")
    println("EC verified encoded: $ecVerified2")

    val ecSignature3 = ec.sign(text, ec.keyOrSigToString(ecKeyPair.private.encoded),"ECDSA")
    val ecVerified3 = ec.verify(
        text,
        ec.keyOrSigToString(ecSignature3),
        ec.keyOrSigToString(ecKeyPair.public.encoded), "ECDSA"
    )
    println("EC verified string: $ecVerified3")
}