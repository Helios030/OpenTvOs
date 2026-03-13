package con.open.tvos.server

import fi.iki.elonen.NanoHTTPD

/**
 * @author pj567
 * @date :2021/1/5
 * @description: 响应按键和输入
 */
class InputRequestProcess(private val remoteServer: RemoteServer) : RequestProcess {

    override fun isRequest(session: NanoHTTPD.IHTTPSession, fileName: String): Boolean {
        if (session.method == NanoHTTPD.Method.POST) {
            when (fileName) {
                "/action" -> return true
            }
        }
        return false
    }

    override fun doResponse(
        session: NanoHTTPD.IHTTPSession,
        fileName: String,
        params: Map<String, String>?,
        files: Map<String, String>?
    ): NanoHTTPD.Response {
        val mDataReceiver = remoteServer.getDataReceiver()
        return when (fileName) {
            "/action" -> {
                if (params?.get("do") != null && mDataReceiver != null) {
                    val action = params["do"]
                    when (action) {
                        "search" -> {
                            mDataReceiver.onTextReceived(params["word"]?.trim() ?: "")
                        }
                        "api" -> {
                            mDataReceiver.onApiReceived(params["url"]?.trim() ?: "")
                        }
                        "push" -> {
                            // 暂未实现
                            mDataReceiver.onPushReceived(params["url"]?.trim() ?: "")
                        }
                    }
                }
                RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "ok")
            }
            else -> {
                RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.NOT_FOUND, "Error 404, file not found.")
            }
        }
    }
}
