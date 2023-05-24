import java.io.*
import java.net.InetAddress
import java.net.Socket
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
        var `in`: BufferedReader? = null
        var out: PrintWriter? = null

        init {
            mailHost = InetAddress.getByName(host)
            localhost = InetAddress.getLocalHost()
            println("mailhost = $mailHost")
            println("localhost= $localhost")
            println("SMTP constructor done\n")
        }

        @Throws(IOException::class)
        fun send(msgFileReader: FileReader?, from: String, to: String): Boolean {
            val smtpPipe: Socket
            val inn: InputStream?
            val outt: OutputStream?
            val msg: BufferedReader
            msg = BufferedReader(msgFileReader)
//            smtpPipe = Socket(mailHost, SMTP_PORT)
            val socketFactory = SSLSocketFactory.getDefault()
            smtpPipe = socketFactory.createSocket(mailHost, SMTP_PORT)
            inn = smtpPipe.getInputStream()
            outt = smtpPipe.getOutputStream()
            `in` = BufferedReader(InputStreamReader(inn))
            out = PrintWriter(OutputStreamWriter(outt), true)
            if (inn == null || outt == null) {
                println("Failed to open streams to socket.")
                return false
            }
            val initialID = `in`!!.readLine()
            println(initialID)
            println("HELO " + localhost.hostName)
            out!!.println("HELO " + localhost.hostName)
            val welcome = `in`!!.readLine()
            println("Server: $welcome")

            println("AUTH PLAIN")
            out!!.println("AUTH PLAIN")
            println("Server: ${`in`!!.readLine()}")

            val encodedCredentails = encodeCredentials("morcinek@student.agh.edu.pl", "")
            println("Encoded: $encodedCredentails")
            out!!.println("$encodedCredentails")
            println("Server: ${`in`!!.readLine()}")

            println("MAIL From:<$from>")
            out!!.println("MAIL From:<$from>")
            val senderOK = `in`!!.readLine()
            println(senderOK)
            println("RCPT TO:<$to>")
            out!!.println("RCPT TO:<$to>")
            val recipientOK = `in`!!.readLine()
            println(recipientOK)
            out!!.println("DATA")
            val afterData = `in`!!.readLine()
            println("Accepted: " + afterData)


            out!!.println("From: $from")
            println("From: $from")

            out!!.println("To: $to")
            println("To: $to")

            out!!.println("Subject: Test message")
            println("Subject: Test message")

            out!!.println("")
            println("")

            var line: String?
            while (msg.readLine().also { line = it } != null) {
                out!!.println(line)
                println(line)
            }
            out!!.println(".")
            println(".")

            val acceptedOK = `in`!!.readLine()
            println("Accepted: " + acceptedOK)
            println("QUIT")
            out!!.println("QUIT")
            return true
        }

        private fun encodeCredentials(login: String, password: String) = Base64.getEncoder().encodeToString("\u0000$login\u0000$password".encodeToByteArray())

        companion object {
            private const val SMTP_PORT = 465
        }
    }
}
