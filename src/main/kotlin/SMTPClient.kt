import java.io.*
import java.net.InetAddress
import java.util.*
import javax.net.ssl.SSLSocketFactory


fun main(args: Array<String>) {

    val smtpClient = SMTPClient("poczta.agh.edu.pl", 465)
    smtpClient.helo()
    smtpClient.login("morcinek@student.agh.edu.pl", "")
    smtpClient.sendEmail("morcinek@student.agh.edu.pl", "tomasz.morcinek@gmail.com", null, null, "New Client working.", FileReader("file.txt"))
}

class SMTPClient(host: String, port: Int) {

    private val socket = SSLSocketFactory.getDefault().createSocket(InetAddress.getByName(host), port)
    private val inputStream by lazy { BufferedReader(InputStreamReader(socket.getInputStream())) }
    private val outputStream by lazy { PrintWriter(OutputStreamWriter(socket.getOutputStream()), true) }
    private val localhost = InetAddress.getLocalHost()

    fun helo() {
        readServerMessage()
        sendMessage("HELO ${localhost.hostName}")
        readServerMessage()
    }

    fun login(login: String, password: String) {
        sendMessage("AUTH PLAIN")
        readServerMessage()
        sendMessage(encodeCredentials(login, password))
        readServerMessage()
    }

    fun sendEmail(from: String, to: String, cc: String?, bcc: String?, subject: String, msgFileReader: FileReader? = null) {
        sendMessage("MAIL From:<$from>")
        readServerMessage()
        sendMessage("RCPT TO:<$to>")
        readServerMessage()
        if (cc != null) {
            sendMessage("RCPT TO:<$cc>")
            readServerMessage()
        }
        if (bcc != null) {
            sendMessage("RCPT TO:<$bcc>")
            readServerMessage()
        }

        sendMessage("DATA")
        readServerMessage()
        sendMessage("From: $from")
        sendMessage("To: $to")
        if (cc != null) {
            sendMessage("Cc: $cc")
        }
        if (bcc != null) {
            sendMessage("Bcc: $bcc")
        }
        sendMessage("Subject: $subject")
        msgFileReader?.let {
            val msg = BufferedReader(msgFileReader)
            var line: String?
            while (msg.readLine().also { line = it } != null) {
                sendMessage(line!!)
            }
        }
        sendMessage(".")
        readServerMessage()
        sendMessage("QUIT")
        readServerMessage()
    }

    private fun sendMessage(message: String) {
        println("Client > $message")
        outputStream.println(message)
    }

    private fun readServerMessage() = println("Server > ${inputStream.readLine()}")

    private fun encodeCredentials(login: String, password: String) = Base64.getEncoder().encodeToString("\u0000$login\u0000$password".encodeToByteArray())
}