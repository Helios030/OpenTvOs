package con.open.tvos.util

import con.open.tvos.crawler.SpiderDebug
import con.open.tvos.server.ControlManager
import con.open.tvos.util.parser.SuperParse
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

object Proxy {

    @JvmStatic
    fun proxy(params: Map<String, String>): Array<Any?>? {
        return try {
            val what = params["go"]
            when (what) {
                "live" -> itv(params)
                "bom" -> removeBOMFromM3U8(params)
                "ad" -> null // TODO
                "SuperParse" -> SuperParse.loadHtml(params["flag"], params["url"])
                else -> null
            }
        } catch (ignored: Throwable) {
            null
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun itv(params: Map<String, String>): Array<Any?>? {
        return try {
            val result = arrayOfNulls<Any>(3)
            var url = params["url"] ?: return null
            val type = params["type"] ?: return null
            url = URLDecoder.decode(url, "UTF-8")

            val client = OkGoHelper.ItvClient
            when (type) {
                "m3u8" -> {
                    val redirectUrl = getRedirectedUrl(url)
                    val request = Request.Builder().url(redirectUrl).build()
                    executeRequest(client, request).use { response ->
                        if (response.isSuccessful) {
                            val respContent = response.body?.string() ?: ""
                            val m3u8Content = processM3u8Content(respContent, redirectUrl)
                            result[0] = 200
                            result[1] = "application/vnd.apple.mpegurl"
                            result[2] = ByteArrayInputStream(m3u8Content.toByteArray())
                        } else {
                            throw IOException("M3U8 Request failed with code: ${response.code}")
                        }
                    }
                }
                "ts" -> {
                    val request = Request.Builder().url(url).build()
                    executeRequest(client, request).use { response ->
                        if (response.isSuccessful) {
                            result[0] = 200
                            result[1] = "video/mp2t"
                            result[2] = ByteArrayInputStream(response.body?.bytes())
                        } else {
                            throw IOException("TS Request failed with code: ${response.code}")
                        }
                    }
                }
                else -> throw IllegalArgumentException("Invalid type: $type")
            }
            result
        } catch (e: Exception) {
            SpiderDebug.log(e)
            null
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun removeBOMFromM3U8(params: Map<String, String>): Array<Any?>? {
        return try {
            val result = arrayOfNulls<Any>(3)
            var url = params["url"] ?: return null
            url = URLDecoder.decode(url, "UTF-8")

            val client = OkGoHelper.ItvClient
            val redirectUrl = getRedirectedUrl(url)
            val request = Request.Builder().url(redirectUrl).build()
            executeRequest(client, request).use { response ->
                if (response.isSuccessful) {
                    var m3u8Content = response.body?.string() ?: ""
                    // 检查并去除 UTF-8 BOM 头（BOM 为 \uFEFF）
                    if (m3u8Content.startsWith("\ufeff")) {
                        m3u8Content = m3u8Content.substring(1)
                    }
                    result[0] = 200
                    result[1] = "application/vnd.apple.mpegurl"
                    result[2] = ByteArrayInputStream(m3u8Content.toByteArray())
                } else {
                    throw IOException("M3U8 Request failed with code: ${response.code}")
                }
            }
            result
        } catch (e: Exception) {
            SpiderDebug.log(e)
            null
        }
    }

    @Throws(IOException::class)
    private fun executeRequest(client: OkHttpClient, request: Request): okhttp3.Response {
        return try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            System.err.println("网络请求异常：${e.message}")
            throw e
        }
    }

    private fun processM3u8Content(m3u8Content: String, m3u8Url: String): String {
        val m3u8Lines = m3u8Content.trim().split("\n")
        val processedM3u8 = StringBuilder()

        for (line in m3u8Lines) {
            if (line.startsWith("#")) {
                processedM3u8.append(line).append("\n")
            } else {
                processedM3u8.append(joinUrl(m3u8Url, line)).append("\n")
            }
        }
        return processedM3u8.toString().replace("\n\n", "\n")
    }

    private fun joinUrl(base: String?, url: String?): String? {
        var baseSafe = base ?: ""
        var urlSafe = url ?: ""
        return try {
            val baseUri = URI(baseSafe.trim())
            urlSafe = urlSafe.trim()
            val urlUri = URI(urlSafe)
            val proxyUrl = ControlManager.get().getAddress(true) + "proxy?go=live&type=ts&url="
            when {
                urlSafe.startsWith("http://") || urlSafe.startsWith("https://") -> 
                    proxyUrl + URLEncoder.encode(urlUri.toString(), "UTF-8")
                urlSafe.startsWith("://") -> 
                    proxyUrl + URLEncoder.encode(URI(baseUri.scheme + urlSafe).toString(), "UTF-8")
                urlSafe.startsWith("//") -> 
                    proxyUrl + URLEncoder.encode(URI(baseUri.scheme + ":" + urlSafe).toString(), "UTF-8")
                else -> {
                    val resolvedUri = baseUri.resolve(urlSafe)
                    proxyUrl + URLEncoder.encode(resolvedUri.toString(), "UTF-8")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getRedirectedUrl(url: String): String {
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isRedirect) {
                return response.header("Location") ?: url
            }
            return url
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getM3U8Content(url: String): String {
        val request = Request.Builder()
            .url(url)
            .build()

        val client = OkGoHelper.ItvClient
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.body?.string() ?: ""
            } else {
                throw IOException("请求失败，HTTP 状态码: ${response.code}")
            }
        }
    }
}
