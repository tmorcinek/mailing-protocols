import java.io.FileReader

fun main(args: Array<String>) {

//    sendEmailWithPlainText()
//    sendEmailWithHtml()

//    readLastEmail(2)
}

private fun sendEmailWithHtml() {
    sendEmailWithText(SMTPClient.Text.HTML("<html><body><b>ala ma kota w boldzie</b></body></html>"))
}

private fun sendEmailWithPlainText() {
    sendEmailWithText(SMTPClient.Text.Plain("<html><body><b>ala ma kota w boldzie</b></body></html>"))
}

private fun sendEmailWithText(text: SMTPClient.Text) {
    SMTPClient("poczta.agh.edu.pl", 465).run {
        helo()
        login("morcinek@student.agh.edu.pl", PASSWORD)
        sendEmail(
            "morcinek@student.agh.edu.pl",
            "morcinek@student.agh.edu.pl",
            "tomasz.morcinek@gmail.com",
            null,
            "Presentation of SMTP client implemented in Kotlin.",
            text,
            FileReader("./file.txt")
        )
    }
}

private fun readLastEmail(numberOfMails: Int = 1) {
    IMAPClient("poczta.agh.edu.pl", 993).run {
        helo()
        noop()
        login("morcinek@student.agh.edu.pl", PASSWORD)
        list()
        var last_id = select("INBOX").toInt()
        (0 until numberOfMails).forEach {
            println("--------------------printing mail with id=${last_id - it} --------------------")
            println(get_mail("INBOX", (last_id - it).toString()))
            println("--------------------end printing email --------------------")
        }
    }
}
