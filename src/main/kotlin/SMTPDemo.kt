import java.io.*
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import javax.net.ssl.SSLSocketFactory

object SMTPDemo {

    @Throws(IOException::class, UnknownHostException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val from = "morcinek@student.agh.edu.pl"
        val to = "tomasz.morcinek@gmail.com"
        val mailHost = "poczta.agh.edu.pl"
        val mail = SMTP(mailHost)
        if (mail.send(FileReader("file.txt"), from, to)) {
            println("Mail sent.")
        } else {
            println("Connect to SMTP server failed!")
        }
        println("Done.")
    }

    internal class SMTP(host: String?) {
        var mailHost: InetAddress
        var localhost: InetAddress
        var inputStream: BufferedReader? = null
        var outputStream: PrintWriter? = null

        init {
            mailHost = InetAddress.getByName(host)
            localhost = InetAddress.getLocalHost()
            println("mailhost = $mailHost")
            println("localhost= $localhost")
            println("SMTP constructor done\n")
        }

        @Throws(IOException::class)
        fun send(msgFileReader: FileReader?, from: String, to: String): Boolean {
            val msg = BufferedReader(msgFileReader)
            val smtpPipe = SSLSocketFactory.getDefault().createSocket(mailHost, SMTP_PORT)
            inputStream = BufferedReader(InputStreamReader(smtpPipe.getInputStream()))
            outputStream = PrintWriter(OutputStreamWriter(smtpPipe.getOutputStream()), true)
            val initialID = inputStream!!.readLine()
            println(initialID)
            println("HELO " + localhost.hostName)
            outputStream!!.println("HELO " + localhost.hostName)
            val welcome = inputStream!!.readLine()
            println("Server: $welcome")

            println("AUTH PLAIN")
            outputStream!!.println("AUTH PLAIN")
            println("Server: ${inputStream!!.readLine()}")

            val encodedCredentails = encodeCredentials("morcinek@student.agh.edu.pl", "")
            println("Encoded: $encodedCredentails")
            outputStream!!.println("$encodedCredentails")
            println("Server: ${inputStream!!.readLine()}")

            println("MAIL From:<$from>")
            outputStream!!.println("MAIL From:<$from>")
            val senderOK = inputStream!!.readLine()
            println(senderOK)
            println("RCPT TO:<$to>")
            outputStream!!.println("RCPT TO:<$to>")
            val recipientOK = inputStream!!.readLine()
            println(recipientOK)
            outputStream!!.println("DATA")
            val afterData = inputStream!!.readLine()
            println("Accepted: " + afterData)


            outputStream!!.println("From: $from")
            println("From: $from")

            outputStream!!.println("To: $to")
            println("To: $to")

            outputStream!!.println("Subject: New Message")
            println("Subject: Test message")

            outputStream!!.println("")
            println("")

            var line: String?
            while (msg.readLine().also { line = it } != null) {
                outputStream!!.println(line)
                println(line)
            }
            outputStream!!.println(".")
            println(".")

            val acceptedOK = inputStream!!.readLine()
            println("Accepted: " + acceptedOK)
            println("QUIT")
            outputStream!!.println("QUIT")
            return true
        }

        private fun encodeCredentials(login: String, password: String) =
            Base64.getEncoder().encodeToString("\u0000$login\u0000$password".encodeToByteArray())

        companion object {
            private const val SMTP_PORT = 465
        }
    }
}
