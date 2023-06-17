import java.io.*
import java.net.InetAddress
import java.util.*
import javax.net.ssl.SSLSocketFactory


fun main(args: Array<String>) {

    val smtpClient = SMTPClient("poczta.agh.edu.pl", 465)
    smtpClient.helo()
    smtpClient.login("morcinek@student.agh.edu.pl", PASSWORD)
    smtpClient.sendEmail("morcinek@student.agh.edu.pl", "morcinek@student.agh.edu.pl", null, null, "New Client working.", SMTPClient.Text.HTML("<html><body><b>ala ma kota w boldzie</b></body></html>"), FileReader("./file.txt"))
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

    fun sendEmail(from: String, to: String, cc: String?, bcc: String?, subject: String, text: Text, msgFileReader: FileReader? = null) {
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
        sendMessage(
            "Content-Type: multipart/alternative; boundary=sep\n\n" +
                    "--sep\n" +
                    "Content-Type: text/${text.contentType}; charset=utf-8\n\n"+
                    "${text.text}\n" +
                    "--sep"
        )
        if (msgFileReader != null) {
            sendMessage(
                    "Content-Type: text/plain; charset=\"iso-8859-1\"\n" +
                    "Content-Disposition: attachment; filename=\"text.txt\"\n" +
                    "Content-Transfer-Encoding: 8bit\n\n")
            msgFileReader.let {
                val msg = BufferedReader(msgFileReader)
                var line: String?
                while (msg.readLine().also { line = it } != null) {
                    sendMessage(line!!)
                }
            }
            sendMessage("--sep--\n")
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

    sealed class Text(val text: String, val contentType: String){
        class HTML(text: String): Text(text, "html")
        class Plain(text: String): Text(text, "plain")
    }
}