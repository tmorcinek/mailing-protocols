import java.util.Base64

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    val login = "uzytkownik"
    val password = "haslo"
    val encoded = encodeCredentials(login, password)
    println("Before: $login, $password, encoded: $encoded")
}

private fun encodeCredentials(login: String, password: String) = Base64.getEncoder().encodeToString("\u0000$login\u0000$password".encodeToByteArray())