package con.open.tvos.server

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import android.util.Base64
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.App
import con.open.tvos.event.RefreshEvent
import con.open.tvos.event.ServerEvent
import con.open.tvos.util.FileUtils
import con.open.tvos.util.OkGoHelper
import con.open.tvos.util.Proxy
import com.github.tvbox.osc.util.RegexUtils.getPattern
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fi.iki.elonen.NanoHTTPD
import org.greenrobot.eventbus.EventBus
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
class RemoteServer(port: Int, private val mContext: Context) : NanoHTTPD(port) {
    private var isStarted = false
    private var mDataReceiver: DataReceiver? = null
    private val getRequestList = ArrayList<RequestProcess>()
    private val postRequestList = ArrayList<RequestProcess>()

    init {
        addGetRequestProcess()
        addPostRequestProcess()
    }

    private fun addGetRequestProcess() {
        getRequestList.add(RawRequestProcess(mContext, "/", R.raw.index, NanoHTTPD.MIME_HTML))
        getRequestList.add(RawRequestProcess(mContext, "/index.html", R.raw.index, NanoHTTPD.MIME_HTML))
        getRequestList.add(RawRequestProcess(mContext, "/style.css", R.raw.style, "text/css"))
        getRequestList.add(RawRequestProcess(mContext, "/ui.css", R.raw.ui, "text/css"))
        getRequestList.add(RawRequestProcess(mContext, "/jquery.js", R.raw.jquery, "application/x-javascript"))
        getRequestList.add(RawRequestProcess(mContext, "/script.js", R.raw.script, "application/x-javascript"))
        getRequestList.add(RawRequestProcess(mContext, "/favicon.ico", R.drawable.app_icon, "image/x-icon"))
    }

    private fun addPostRequestProcess() {
        postRequestList.add(InputRequestProcess(this))
    }

    @Throws(IOException::class)
    override fun start(timeout: Int, daemon: Boolean) {
        isStarted = true
        super.start(timeout, daemon)
        EventBus.getDefault().post(ServerEvent(ServerEvent.SERVER_SUCCESS))
    }

    override fun stop() {
        super.stop()
        isStarted = false
    }

    private fun getProxy(rs: Array<Any?>): Response {
        return try {
            if (rs[0] is NanoHTTPD.Response) return rs[0] as NanoHTTPD.Response
            val code = rs[0] as Int
            val mime = rs[1] as String
            val stream = rs[2] as? InputStream?
            val response = NanoHTTPD.newChunkedResponse(
                Response.Status.lookup(code),
                mime,
                stream
            )
            // 添加头部信息
            if (rs.size >= 4 && rs[3] is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val mapHeader = rs[3] as Map<String, String>
                if (mapHeader.isNotEmpty()) {
                    for ((key, value) in mapHeader) {
                        response.addHeader(key, value)
                    }
                }
            }
            response
        } catch (th: Throwable) {
            NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "500")
        }
    }

    override fun serve(session: IHTTPSession): Response {
        EventBus.getDefault().post(ServerEvent(ServerEvent.SERVER_CONNECTION))
        if (session.uri.isNotEmpty()) {
            var fileName = session.uri.trim()
            if (fileName.indexOf('?') >= 0) {
                fileName = fileName.substring(0, fileName.indexOf('?'))
            }
            if (session.method == Method.GET) {
                for (process in getRequestList) {
                    if (process.isRequest(session, fileName)) {
                        return process.doResponse(session, fileName, session.parms, null)
                    }
                }
                if (fileName == "/proxy") {
                    val params = session.parms
                    params.putAll(session.headers)
                    if (params.containsKey("do")) {
                        val rs = ApiConfig.get().proxyLocal(params)
                        return getProxy(rs)
                    }
                    if (params.containsKey("go")) {
                        val rs = Proxy.proxy(params)
                        return getProxy(rs)
                    }
                } else if (fileName.startsWith("/file/")) {
                    try {
                        val f = fileName.substring(6)
                        val root = Environment.getExternalStorageDirectory().absolutePath
                        val file = "$root/$f"
                        val localFile = File(file)
                        if (localFile.exists()) {
                            return if (localFile.isFile) {
                                NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, "application/octet-stream", FileInputStream(localFile))
                            } else {
                                NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, fileList(root, f))
                            }
                        } else {
                            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "File $file not found!")
                        }
                    } catch (th: Throwable) {
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, th.message)
                    }
                } else if (fileName == "/dns-query") {
                    val name = session.parms["name"]
                    var rs: ByteArray? = null
                    try {
                        rs = OkGoHelper.dnsOverHttps.lookupHttpsForwardSync(name)
                    } catch (th: Throwable) {
                        rs = ByteArray(0)
                    }
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/dns-message", ByteArrayInputStream(rs), rs!!.size.toLong())
                } else if (fileName.startsWith("/push/")) {
                    var url = fileName.substring(6)
                    if (url.startsWith("b64:")) {
                        try {
                            url = String(Base64.decode(url.substring(4), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), "UTF-8")
                        } catch (e: UnsupportedEncodingException) {
                            e.printStackTrace()
                        }
                    } else {
                        url = URLDecoder.decode(url)
                    }
                    EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_PUSH_URL, url))
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "ok")
                } else if (fileName.startsWith("/proxyM3u8")) {
                    // com.github.tvbox.osc.util.LOG.i("echo-m3u8:"+m3u8Content)
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, m3u8Content)
                } else if (fileName.startsWith("/dash/")) {
                    val dashData = App.getInstance().dashData
                    try {
                        val data = String(Base64.decode(dashData, Base64.DEFAULT or Base64.NO_WRAP), "UTF-8")
                        return NanoHTTPD.newFixedLengthResponse(
                            Response.Status.OK,
                            "application/dash+xml",
                            data
                        )
                    } catch (th: Throwable) {
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, dashData)
                    }
                }
            } else if (session.method == Method.POST) {
                val files = HashMap<String, String>()
                try {
                    if (session.headers.containsKey("content-type")) {
                        val hd = session.headers["content-type"]
                        if (hd != null) {
                            // cuke: 修正中文乱码问题
                            if (hd.lowercase().contains("multipart/form-data") && !hd.lowercase().contains("charset=")) {
                                val matcher = getPattern("[ |\\t]*(boundary[ |\\t]*=[ |\\t]*['|\"]?[^\"'^;^,]*['|\"]?)", RegexOption.IGNORE_CASE).matcher(hd)
                                val boundary = if (matcher.find()) matcher.group(1) else null
                                if (boundary != null) {
                                    session.headers["content-type"] = "multipart/form-data; charset=utf-8; $boundary"
                                }
                            }
                        }
                    }
                    session.parseBody(files)
                } catch (IOExc: IOException) {
                    return createPlainTextResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + IOExc.message)
                } catch (rex: ResponseException) {
                    return createPlainTextResponse(rex.status, rex.message)
                }
                for (process in postRequestList) {
                    if (process.isRequest(session, fileName)) {
                        return process.doResponse(session, fileName, session.parms, files)
                    }
                }
                try {
                    val params = session.parms
                    if (fileName == "/upload") {
                        val path = params["path"]
                        for ((k, tmpFile) in files) {
                            if (k.startsWith("files-")) {
                                val fn = params[k]
                                val tmp = File(tmpFile)
                                val root = Environment.getExternalStorageDirectory().absolutePath
                                val file = File("$root/$path/$fn")
                                if (file.exists()) file.delete()
                                if (tmp.exists()) {
                                    if (fn?.lowercase()?.endsWith(".zip") == true) {
                                        unzip(tmp, "$root/$path")
                                    } else {
                                        FileUtils.copyFile(tmp, file)
                                    }
                                }
                                if (tmp.exists()) tmp.delete()
                            }
                        }
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "OK")
                    } else if (fileName == "/newFolder") {
                        val path = params["path"]
                        val name = params["name"]
                        val root = Environment.getExternalStorageDirectory().absolutePath
                        val file = File("$root/$path/$name")
                        if (!file.exists()) {
                            file.mkdirs()
                            val flag = File("$root/$path/$name/.tvbox_folder")
                            if (!flag.exists()) flag.createNewFile()
                        }
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "OK")
                    } else if (fileName == "/delFolder") {
                        val path = params["path"]
                        val root = Environment.getExternalStorageDirectory().absolutePath
                        val file = File("$root/$path")
                        if (file.exists()) {
                            FileUtils.recursiveDelete(file)
                        }
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "OK")
                    } else if (fileName == "/delFile") {
                        val path = params["path"]
                        val root = Environment.getExternalStorageDirectory().absolutePath
                        val file = File("$root/$path")
                        if (file.exists()) {
                            file.delete()
                        }
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "OK")
                    }
                } catch (th: Throwable) {
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "OK")
                }
            }
        }
        //default page: index.html
        return getRequestList[0].doResponse(session, "", null, null)
    }

    fun setDataReceiver(receiver: DataReceiver?) {
        mDataReceiver = receiver
    }

    fun getDataReceiver(): DataReceiver? {
        return mDataReceiver
    }

    fun isStarting(): Boolean {
        return isStarted
    }

    fun getServerAddress(): String {
        val ipAddress = getLocalIPAddress(mContext)
        return "http://$ipAddress:$serverPort/"
    }

    fun getLoadAddress(): String {
        return "http://127.0.0.1:$serverPort/"
    }

    private fun fileTime(time: Long, fmt: String): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        val date = calendar.time
        val sdf = SimpleDateFormat(fmt)
        return sdf.format(date)
    }

    private fun fileList(root: String, path: String): String {
        val file = File("$root/$path")
        val list = file.listFiles()
        val info = JsonObject()
        info.addProperty("remote", getServerAddress().replace("http://", "clan://"))
        info.addProperty("del", 0)
        if (path.isEmpty()) {
            info.addProperty("parent", ".")
        } else {
            info.addProperty("parent", file.parentFile?.absolutePath?.replace("$root/", "")?.replace(root, ""))
        }
        if (list == null || list.isEmpty()) {
            info.add("files", JsonArray())
            return info.toString()
        }
        list.sortWith { o1, o2 ->
            if (o1.isDirectory && o2.isFile) -1
            else if (o1.isFile && o2.isDirectory) 1
            else o1.name.compareTo(o2.name)
        }
        val result = JsonArray()
        for (f in list) {
            if (f.name.startsWith(".")) {
                if (f.name == ".tvbox_folder") {
                    info.addProperty("del", 1)
                }
                continue
            }
            val fileObj = JsonObject()
            fileObj.addProperty("name", f.name)
            fileObj.addProperty("path", f.absolutePath.replace("$root/", ""))
            fileObj.addProperty("time", fileTime(f.lastModified(), "yyyy/MM/dd aHH:mm:ss"))
            fileObj.addProperty("dir", if (f.isDirectory) 1 else 0)
            result.add(fileObj)
        }
        info.add("files", result)
        return info.toString()
    }

    @Throws(Throwable::class)
    private fun unzip(zipFilePath: File, destDirectory: String) {
        val destDir = File(destDirectory)
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val zip = ZipFile(zipFilePath)
        @Suppress("UNCHECKED_CAST")
        val iter = zip.entries() as Enumeration<ZipEntry>
        while (iter.hasMoreElements()) {
            val entry = iter.nextElement()
            val `is` = zip.getInputStream(entry)
            val filePath = destDirectory + File.separator + entry.name
            if (!entry.isDirectory) {
                extractFile(`is`, filePath)
            } else {
                val dir = File(filePath)
                if (!dir.exists()) dir.mkdirs()
                val flag = File("$dir/.tvbox_folder")
                if (!flag.exists()) flag.createNewFile()
            }
        }
    }

    @Throws(Throwable::class)
    private fun extractFile(inputStream: InputStream, destFilePath: String) {
        val dst = File(destFilePath)
        if (dst.exists()) dst.delete()
        val bos = BufferedOutputStream(FileOutputStream(destFilePath))
        val bytesIn = ByteArray(2048)
        var len = inputStream.read(bytesIn)
        while (len > 0) {
            bos.write(bytesIn, 0, len)
            len = inputStream.read(bytesIn)
        }
        bos.close()
    }

    companion object {
        var serverPort = 9978
        var m3u8Content: String? = null

        fun createPlainTextResponse(status: Response.IStatus, text: String): Response {
            return newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, text)
        }

        fun createJSONResponse(status: Response.IStatus, text: String): Response {
            return newFixedLengthResponse(status, "application/json", text)
        }

        @SuppressLint("DefaultLocale")
        fun getLocalIPAddress(context: Context): String {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            if (ipAddress == 0) {
                try {
                    val enumerationNi = NetworkInterface.getNetworkInterfaces()
                    while (enumerationNi.hasMoreElements()) {
                        val networkInterface = enumerationNi.nextElement()
                        val interfaceName = networkInterface.displayName
                        if (interfaceName == "eth0" || interfaceName == "wlan0") {
                            val enumIpAddr = networkInterface.inetAddresses
                            while (enumIpAddr.hasMoreElements()) {
                                val inetAddress = enumIpAddr.nextElement()
                                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                                    return inetAddress.hostAddress
                                }
                            }
                        }
                    }
                } catch (e: SocketException) {
                    e.printStackTrace()
                }
            } else {
                return String.format("%d.%d.%d.%d", ipAddress and 0xff, ipAddress shr 8 and 0xff, ipAddress shr 16 and 0xff, ipAddress shr 24 and 0xff)
            }
            return "0.0.0.0"
        }
    }
}
