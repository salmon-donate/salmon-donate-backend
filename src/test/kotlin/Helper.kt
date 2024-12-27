import kotlin.random.Random

private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

internal fun genRandomString(length: Int = Random.nextInt(1, 100)) : String {
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

internal fun genRandomHexString(length: Int = Random.nextInt(1, 100)) : String {
    return genRandomString(length).toByteArray()
        .joinToString("") {
            "%02x".format(it)
        }
            .take(length)
}

internal fun getEnvOrThrow(name: String): String {
    val value = System.getenv(name)
    if (value.isEmpty()) throw IllegalArgumentException("Env var $name must be specified")
    return value
}