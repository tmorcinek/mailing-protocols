import java.io.*
import java.net.InetAddress
import java.util.*
import javax.net.ssl.SSLSocketFactory
import java.util.Base64

fun main(args: Array<String>) {

    val imapClient = IMAPClient("poczta.agh.edu.pl", 993)
    imapClient.helo()
    imapClient.noop()
    imapClient.login("bprzechera@student.agh.edu.pl", "")
    imapClient.list()

    var last_id=imapClient.select("INBOX")
    val email:Email=imapClient.get_mail("INBOX",last_id)
    println(email)

}
data class Email(var from:String,
                 var to:String,
                 var date:String,
                 var text:String,
                 var html:String,
                 var filelist:ArrayList<String>)
class IMAPClient(host: String, port: Int) {

    private val socket = SSLSocketFactory.getDefault().createSocket(InetAddress.getByName(host), port)
    private val inputStream by lazy { BufferedReader(InputStreamReader(socket.getInputStream())) }
    private val outputStream by lazy { PrintWriter(OutputStreamWriter(socket.getOutputStream()), true) }
    private val localhost = InetAddress.getLocalHost()
    private var commandCounter=0

    fun saveBase64StringToFile(
        base64Str: String?,
        filePath: String?, fileName: String?
    ) {
        var bos: BufferedOutputStream? = null
        var fos: FileOutputStream? = null
        var file: File? = null
        try {
            val dir = File(filePath)
            if (!dir.exists() && dir.isDirectory) {
                dir.mkdirs()
            }
            file = File(filePath, fileName)
            fos = FileOutputStream(file)
            bos = BufferedOutputStream(fos)
            val bfile: ByteArray = Base64.getDecoder().decode(base64Str)
            bos.write(bfile)
        }
        finally {
            if (bos != null) {
                try {
                    bos.close()
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }
            }
            if (fos != null) {
                try {
                    fos.close()
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }
            }
        }
    }
    fun helo(doPrint: Boolean =true) {
        readServerMessage(doPrint)
        sendMessage("STARTTLS",doPrint)
        readServerMessage(doPrint)
//        sendMessage("CAPABILITY")
//        readServerMessage()
    }
    fun select(name:String,doPrint: Boolean =true):String{
        var count:String="NOT FOUND"
        val countRegex = "\\* [0-9]+ EXISTS".toRegex()
        val messageId=sendMessage("SELECT $name",doPrint)
        var lastMessage:String=""
        while (!(lastMessage.contains(messageId, ignoreCase = true)&& lastMessage.contains("SELECT completed", ignoreCase = true)))  {
            lastMessage=readServerMessage(doPrint)
            val regex_temp=countRegex.find(lastMessage)
            if (regex_temp!=null){
                count=regex_temp.value.replace("EXISTS","").replace("*","").replace(" ","")
            }
        }
        return count
    }
    fun process_email(body_text:String,header_from:String="",header_to:String="",header_date:String=""):Email{
        val fromRegex = """From:.*\n""".toRegex()
        val toRegex = """To:.*\n""".toRegex()
        val dateRegex = """Date:.*\n""".toRegex()
        val boundaryRegex = "--.*=_.*\n".toRegex()
        val contentTypeRegex="Content-Type:.*;".toRegex()
        val contentEncodingRegex="Content-Transfer-Encoding:.*\n".toRegex()
        val contentNameRegex="name=\".*\"\n".toRegex()
        var from:String=""
        var to:String=""
        var date:String=""
        var text:String=""
        var html:String=""
        var filelist:ArrayList<String> = ArrayList<String>()

        var header_from_temp=fromRegex.find(header_from)
        if (header_from_temp != null)
            from=header_from_temp.value.replace("From: ","").replace("\n","")
        var header_to_temp=toRegex.find(header_to)
        if (header_to_temp != null)
            to=header_to_temp.value.replace("To: ","").replace("\n","")
        var header_date_temp=dateRegex.find(header_date)
        if (header_date_temp != null)
            date=header_date_temp.value.replace("Date: ","").replace("\n","")


        var boundary_temp=boundaryRegex.find(body_text)
        var boundary:String
        if (boundary_temp == null)
            boundary=""
        else
            boundary=boundary_temp.value.replace("boundary=","").replace("\"","").replace("\n","")
        if (boundary!="") {
            var text_pieces: List<String> = body_text.split(boundary)

            for (tp in text_pieces) {
                var temp = contentTypeRegex.find(tp)
                if (temp != null) {
                    var contentType: String = temp.value.replace("Content-Type: ", "").split(";")[0]
//                println(tp)
//                println(contentType)
                    when (contentType) {
                        "text/plain" -> {
                            val name_temp = contentNameRegex.find(tp)
                            if (name_temp != null) {
                                var name = name_temp.value.replace("name=\"", "").replace("\"\n", "")
                                var content = tp.split("\n\n").drop(1).joinToString("\n\n")
                                File("./" + name).writeText(content)
                                filelist.add("./" + name)
//                            println("------------------content-----------------")
//                            println(content)
//                            println("------------------content-----------------")
                            } else {
                                text += tp.split("[\r\n]{2}".toRegex()).drop(1).joinToString("\n\n")
//                            println(text)
                            }
                        }
                        "text/html" -> {
                            html += tp.split("[\r\n]{2}".toRegex()).drop(1).joinToString("\n")
                        }
                        "multipart/mixed" -> {
                            val emin: Email = process_email(tp, tp, tp, tp)
                            text += emin.text
                            html += emin.html
                            filelist.addAll(emin.filelist)
                        }
                        else -> {
                            if (contentType.contains("application", ignoreCase = true) || contentType.contains(
                                    "image",
                                    ignoreCase = true
                                ) || contentType.contains("video", ignoreCase = true)
                            ) {
                                val encoding_temp = contentEncodingRegex.find(tp)
                                val name_temp = contentNameRegex.find(tp)
                                val boundary_temp = boundaryRegex.find(tp)
                                if (encoding_temp != null && name_temp != null && boundary_temp != null) {
                                    var encoding =
                                        encoding_temp.value.replace("Content-Transfer-Encoding: ", "").replace("\n", "")
                                    if (encoding == "base64") {
                                        var name = name_temp.value.replace("name=\"", "").replace("\"\n", "")
                                        var fileBoundary =
                                            boundary_temp.value.replace("Content-Transfer-Encoding: ", "")
                                                .replace("\n", "")
                                        var file =
                                            tp.split("\n\n")[tp.split("\n\n").size - 1].split(fileBoundary)[0].replace(
                                                "\n",
                                                ""
                                            )
                                        saveBase64StringToFile(file, "./", name)
                                        filelist.add("./" + name)
                                    }

//                                println(encoding)
                                }

                            }
                        }
                    }
                }
            }
        }else{

            text=body_text.split("\n").drop(1).dropLast(2).joinToString("\n")
        }
//        println("------------------body_text-----------------")
//        println(body_text)
//        println("------------------body_text-----------------")
//        println(emaillist)
//        println(text_pieces[1])
        return Email(from,to,date,text,html,filelist)
    }
    fun get_mail(box_name:String,mail_id:String):Email{


        select(box_name,false)

        var header_from=fetch(mail_id,"BODY[HEADER.FIELDS (FROM)]",false)
        var header_to=fetch(mail_id,"BODY[HEADER.FIELDS (TO)]",false)
        var header_date=fetch(mail_id,"BODY[HEADER.FIELDS (DATE)]",false)
        var body_text=fetch(mail_id,"RFC822.TEXT",false)
        return process_email(body_text,header_from,header_to,header_date)

//        boundary="--"+boundary
//        println(boundary_temp)
//        println(boundary)
//        Thread.sleep(1_000)
//        println("-----------------------------------------------------------")
//        println(temp_text)
//
//        println("-----------------------------------------------------------")

//        println(text_pieces.size)

    }
    fun fetch(id:String,whatToGet:String="RFC822.TEXT",doPrint: Boolean =true):String{
        val messageId=sendMessage("FETCH $id $whatToGet",doPrint)
        var all_messages:String=""
        var lastMessage:String=""
        while (!(lastMessage.contains(messageId, ignoreCase = true)&& lastMessage.contains("FETCH completed", ignoreCase = true)))  {
            lastMessage=readServerMessage(doPrint)
            if (all_messages!="")
                all_messages+="\n"
            all_messages+=lastMessage
        }
        return all_messages
    }
    fun list(doPrint: Boolean =true):String{
        val messageId=sendMessage("LIST \"*\" \"*\"",doPrint)
        var all_messages:String=""
        var lastMessage:String=""
        while (!(lastMessage.contains(messageId, ignoreCase = true)&& lastMessage.contains("LIST completed", ignoreCase = true)))  {
            lastMessage=readServerMessage(doPrint)
            if (all_messages!="")
                all_messages+="\n"
            all_messages+=lastMessage
        }
        return all_messages
    }
    fun login(login: String, password: String,doPrint: Boolean =true) {
        sendMessage("LOGIN $login $password",doPrint)
        readServerMessage(doPrint)
    }
    fun noop(doPrint: Boolean =true) {
        sendMessage("NOOP",doPrint)
        readServerMessage(doPrint)
    }
    private fun sendMessage(message: String,doPrint: Boolean =true):String {
        commandCounter+=1
        if (doPrint)
            println("Client > a$commandCounter $message")
        outputStream.println("a$commandCounter $message")
        return "a$commandCounter"
    }
    private fun readServerMessage(doPrint: Boolean =true):String {
        val message=inputStream.readLine()
        if (doPrint)
            println("Server > $message")
        return message
    }
}