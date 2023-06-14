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
//    imapClient.select("INBOX")
//    imapClient.fetch("530")
    imapClient.select("Sent")
    imapClient.get_mail("Sent","50")
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
    fun select(name:String,doPrint: Boolean =true){
        val messageId=sendMessage("SELECT $name",doPrint)
        var lastMessage:String=""
        while (!(lastMessage.contains(messageId, ignoreCase = true)&& lastMessage.contains("SELECT completed", ignoreCase = true)))  {
            lastMessage=readServerMessage(doPrint)
        }
    }
    fun get_mail(box_name:String,mail_id:String):Email{
        val fromRegex = """From:.*\n""".toRegex()
        val toRegex = """To:.*\n""".toRegex()
        val dateRegex = """Date:.*\n""".toRegex()
        val boundaryRegex = "boundary=\".*\"\n".toRegex()
        val contentTypeRegex="Content-Type:.*;".toRegex()
        val fileBoundary_regex="--=_.*\n".toRegex()
        val contentEncodingRegex="Content-Transfer-Encoding:.*\n".toRegex()
        val contentNameRegex="name=\".*\"\n".toRegex()
        var from:String=""
        var to:String=""
        var date:String=""
        var text:String=""
        var html:String=""
        var filelist:ArrayList<String> = ArrayList<String>()
        select(box_name,false)

        var temp=fromRegex.find(fetch(mail_id,"BODY[HEADER.FIELDS (FROM)]",false))
        if (temp != null)
            from=temp.value.replace("From: ","").replace("\n","")
        temp=null
        temp=toRegex.find(fetch(mail_id,"BODY[HEADER.FIELDS (TO)]",false))
        if (temp != null)
            to=temp.value.replace("To: ","").replace("\n","")
        temp=null
        temp=dateRegex.find(fetch(mail_id,"BODY[HEADER.FIELDS (DATE)]",false))
        if (temp != null)
            date=temp.value.replace("To: ","").replace("\n","")
        temp=null
        var temp_text=fetch(mail_id,"RFC822.TEXT",false)
        var boundary_temp=boundaryRegex.find(temp_text)
        var boundary:String
        if (boundary_temp == null)
            boundary=""
        else
            boundary=boundary_temp.value.replace("boundary=","").replace("\"","").replace("\n","")
        boundary="--"+boundary
        var text_pieces:List<String> =temp_text.split(boundary)
//        println(text_pieces.size)
        for (tp in text_pieces) {
            temp=contentTypeRegex.find(tp)
            if (temp!=null) {
                var contentType:String=temp.value.replace("Content-Type: ","").split(";")[0]
//                println(contentType)
                when (contentType) {
                    "text/plain"->{
                        text=tp.split("[\r\n]{3}".toRegex()).drop(1).joinToString("\n\n\n")
                    }
                    "text/html"->{
//                        println(tp)
                        html=tp.split("[\r\n]{2}".toRegex()).drop(1).joinToString("\n")
                    }
                    else->{
                        if (contentType.contains("application", ignoreCase = true)||contentType.contains("image", ignoreCase = true)||contentType.contains("video", ignoreCase = true)) {
                            val encoding_temp=contentEncodingRegex.find(tp)
                            val name_temp=contentNameRegex.find(tp)
                            val boundary_temp=fileBoundary_regex.find(tp)
                            if (encoding_temp!=null && name_temp != null && boundary_temp!=null){
                                var encoding=encoding_temp.value.replace("Content-Transfer-Encoding: ","").replace("\n","")
                                if (encoding=="base64") {
                                    var name = name_temp.value.replace("name=\"", "").replace("\"\n", "")
                                    var fileBoundary =
                                        boundary_temp.value.replace("Content-Transfer-Encoding: ", "").replace("\n", "")
                                    var file =
                                        tp.split("\n\n")[tp.split("\n\n").size - 1].split(fileBoundary)[0].replace(
                                            "\n",
                                            ""
                                        )
                                    saveBase64StringToFile(file, "./", name)
                                    filelist.add("./"+ name)
                                }

//                                println(encoding)
                            }

                        }
                    }
                }
            }
        }
        println(from)
        println(to)
        println(date)
//        println(boundary)
        println(text)
        println(html)
        println(filelist)
//        println(text_pieces[1])
        return Email(from,to,date,boundary,html,filelist)
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