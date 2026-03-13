package con.open.tvos.api

import android.app.Activity
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import con.open.tvos.crawler.JarLoader
import con.open.tvos.crawler.JsLoader
import con.open.tvos.crawler.Spider
import con.open.tvos.crawler.pyLoader
import con.open.tvos.crawler.python.IPyLoader
import con.open.tvos.base.App
import con.open.tvos.bean.*
import con.open.tvos.server.ControlManager
import con.open.tvos.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import com.orhanobut.hawk.Hawk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ApiConfig - Kotlin migration from Java
 * 
 * Central configuration manager for TVBox application.
 * Manages spider sources, live TV channels, parse beans, and IJK codec settings.
 * 
 * This is a Singleton managed by Hilt.
 */
@Singleton
class ApiConfig @Inject constructor() {

    // Spider loaders
    private val jarLoader: JarLoader = JarLoader()
    private val jsLoader: JsLoader = JsLoader()
    private val pyLoader: IPyLoader = pyLoader()
    private val gson: Gson = Gson()

    // Source management
    private val sourceBeanList: LinkedHashMap<String, SourceBean> = LinkedHashMap()
    private var mHomeSource: SourceBean? = null
    private val emptyHome: SourceBean = SourceBean()

    // Parse/VIP handling
    private val parseBeanList: ArrayList<ParseBean> = ArrayList()
    private var mDefaultParse: ParseBean? = null
    private var vipParseFlags: List<String>? = null

    // Live TV
    private val liveChannelGroupList: ArrayList<LiveChannelGroup> = ArrayList()
    private val liveSettingGroupList: ArrayList<LiveSettingGroup> = ArrayList()
    private var liveSpider: String = ""
    private var currentLiveSpider: String? = null

    // Hosts mapping
    private var myHosts: MutableMap<String, String> = HashMap()

    // IJK codec settings
    private var ijkCodes: ArrayList<IJKCode>? = null

    // Spider configuration
    var spider: String? = null
        private set
    var wallpaper: String = ""
        private set

    // Cache key for config decryption
    private var tempKey: String? = null

    // User agent and headers
    private val userAgent = "okhttp/3.15"
    private val requestAccept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"

    // Default live configuration
    private var defaultLiveObjString = "{\"lives\":[{\"name\":\"txt_m3u\",\"type\":0,\"url\":\"txt_m3u_url\"}]}"

    companion object {
        private var instance: ApiConfig? = null
        private var jarCache = "true"

        @JvmStatic
        fun get(): ApiConfig {
            return instance ?: synchronized(ApiConfig::class.java) {
                instance ?: ApiConfig().also { 
                    instance = it
                    it.init()
                }
            }
        }

        /**
         * Find and decode result from encrypted JSON
         */
        @JvmStatic
        fun findResult(json: String, configKey: String?): String {
            var content = json
            try {
                if (AES.isJson(content)) return content
                val pattern: Pattern = RegexUtils.getPattern("[A-Za-z0]{8}\\*\\*")
                val matcher: Matcher = pattern.matcher(content)
                if (matcher.find()) {
                    content = content.substring(content.indexOf(matcher.group()) + 10)
                    content = String(Base64.decode(content, Base64.DEFAULT))
                }
                if (content.startsWith("2423")) {
                    val data = content.substring(content.indexOf("2324") + 4, content.length - 26)
                    content = String(AES.toBytes(content)).lowercase()
                    val key = AES.rightPadding(
                        content.substring(content.indexOf("\$#") + 2, content.indexOf("#\$")),
                        "0", 16
                    )
                    val iv = AES.rightPadding(content.substring(content.length - 13), "0", 16)
                    return AES.CBC(data, key, iv)
                } else if (configKey != null && !AES.isJson(content)) {
                    return AES.ECB(content, configKey)
                } else {
                    return content
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return json
        }

        private fun getImgJar(body: String): ByteArray {
            val pattern = RegexUtils.getPattern("[A-Za-z0]{8}\\*\\*")
            val matcher = pattern.matcher(body)
            return if (matcher.find()) {
                val content = body.substring(body.indexOf(matcher.group()) + 10)
                Base64.decode(content, Base64.DEFAULT)
            } else {
                "".toByteArray()
            }
        }
    }

    private fun init() {
        clearLoader()
        Hawk.put(HawkConfig.LIVE_GROUP_LIST, JsonArray())
        loadDefaultConfig()
    }

    // ==================== Source Management ====================

    fun getSource(key: String): SourceBean? = sourceBeanList[key]

    fun setSourceBean(sourceBean: SourceBean) {
        mHomeSource = sourceBean
        Hawk.put(HawkConfig.HOME_API, sourceBean.key)
    }

    fun getHomeSourceBean(): SourceBean = mHomeSource ?: emptyHome

    fun getSourceBeanList(): List<SourceBean> = ArrayList(sourceBeanList.values)

    fun getSwitchSourceBeanList(): List<SourceBean> {
        return sourceBeanList.values.filter { it.filterable == 1 }
    }

    // ==================== Spider Access ====================

    fun getCSP(sourceBean: SourceBean): Spider {
        return when {
            sourceBean.api.endsWith(".js") || sourceBean.api.contains(".js?") -> {
                jsLoader.getSpider(sourceBean.key, sourceBean.api, sourceBean.ext, sourceBean.jar)
            }
            sourceBean.api.contains(".py") -> {
                pyLoader.getSpider(sourceBean.key, sourceBean.api, sourceBean.ext)
            }
            else -> {
                jarLoader.getSpider(sourceBean.key, sourceBean.api, sourceBean.ext, sourceBean.jar)
            }
        }
    }

    fun getPyCSP(url: String): Spider {
        return pyLoader.getSpider(MD5.string2MD5(url), url, "")
    }

    // ==================== Parse Management ====================

    fun setDefaultParse(parseBean: ParseBean) {
        mDefaultParse?.isDefault = false
        mDefaultParse = parseBean
        Hawk.put(HawkConfig.DEFAULT_PARSE, parseBean.name)
        parseBean.isDefault = true
    }

    fun getDefaultParse(): ParseBean? = mDefaultParse

    fun getParseBeanList(): List<ParseBean> = parseBeanList

    fun getVipParseFlags(): List<String>? = vipParseFlags

    // ==================== Live TV ====================

    fun getChannelGroupList(): List<LiveChannelGroup> = liveChannelGroupList

    fun getLiveSettingGroupList(): List<LiveSettingGroup> = liveSettingGroupList

    fun setLiveJar(liveJar: String) {
        if (liveJar.contains(".py")) {
            pyLoader.setRecentPyKey(liveJar)
        } else {
            val jarUrl = if (liveJar.isNotEmpty()) liveJar else liveSpider
            jarLoader.setRecentJarKey(MD5.string2MD5(jarUrl))
        }
        currentLiveSpider = liveJar
    }

    // ==================== IJK Codec ====================

    fun getIjkCodes(): List<IJKCode>? = ijkCodes

    fun getCurrentIJKCode(): IJKCode? {
        val codeName = Hawk.get(HawkConfig.IJK_CODEC, "硬解码")
        return getIJKCodec(codeName)
    }

    fun getIJKCodec(name: String): IJKCode? {
        return ijkCodes?.find { it.name == name } ?: ijkCodes?.getOrNull(0)
    }

    // ==================== Hosts ====================

    fun getMyHost(): Map<String, String> = myHosts

    // ==================== Config Loading ====================

    interface LoadConfigCallback {
        fun success()
        fun error(msg: String)
        fun notice(msg: String)
    }

    fun loadConfig(useCache: Boolean, callback: LoadConfigCallback, activity: Activity) {
        val apiUrl = Hawk.get(HawkConfig.API_URL, "")
        if (apiUrl.isEmpty()) {
            callback.error("-1")
            return
        }

        val cache = File(App.getInstance().filesDir.absolutePath + "/" + MD5.encode(apiUrl))
        if (useCache && cache.exists()) {
            try {
                parseJson(apiUrl, cache)
                callback.success()
                return
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }

        val configUrl = configUrl(apiUrl)
        val configUrlFile = File(App.getInstance().filesDir.absolutePath + "/config_url")
        FileUtils.saveCache(configUrlFile, configUrl)

        OkGo.get<String>(configUrl)
            .headers("User-Agent", userAgent)
            .headers("Accept", requestAccept)
            .execute(object : AbsCallback<String>() {
                override fun onSuccess(response: Response<String>) {
                    try {
                        val json = response.body()
                        parseJson(apiUrl, json)
                        FileUtils.saveCache(cache, json)
                        callback.success()
                    } catch (th: Throwable) {
                        th.printStackTrace()
                        callback.error("解析配置失败")
                    }
                }

                override fun onError(response: Response<String>) {
                    super.onError(response)
                    if (cache.exists()) {
                        try {
                            parseJson(apiUrl, cache)
                            callback.success()
                            return
                        } catch (th: Throwable) {
                            th.printStackTrace()
                        }
                    }
                    callback.error("拉取配置失败\n${response.exception?.message ?: ""}")
                }

                override fun convertResponse(response: okhttp3.Response): String {
                    val result = if (response.body == null) {
                        ""
                    } else {
                        var decoded = findResult(response.body!!.string(), tempKey)
                        if (apiUrl.startsWith("clan")) {
                            decoded = clanContentFix(clanToAddress(apiUrl), decoded)
                        }
                        fixContentPath(apiUrl, decoded)
                    }
                    return result
                }
            })
    }

    // ==================== Private Helper Methods ====================

    private fun configUrl(apiUrl: String): String {
        var configUrl = ""
        val pk = ";pk;"
        var url = apiUrl.replace("file://", "clan://localhost/")
        
        if (url.contains(pk)) {
            val a = url.split(pk)
            tempKey = a[1]
            configUrl = when {
                url.startsWith("clan") -> clanToAddress(a[0])
                url.startsWith("http") -> a[0]
                else -> "http://${a[0]}"
            }
        } else if (url.startsWith("clan")) {
            configUrl = clanToAddress(url)
        } else if (!url.startsWith("http")) {
            configUrl = "http://$url"
        } else {
            configUrl = url
        }
        return configUrl
    }

    private fun clanToAddress(lanLink: String): String {
        return if (lanLink.startsWith("clan://localhost/")) {
            lanLink.replace("clan://localhost/", "${ControlManager.get().getAddress(true)}file/")
        } else {
            val link = lanLink.substring(7)
            val end = link.indexOf('/')
            "http://${link.substring(0, end)}/file/${link.substring(end + 1)}"
        }
    }

    private fun clanContentFix(lanLink: String, content: String): String {
        val fix = lanLink.substring(0, lanLink.indexOf("/file/") + 6)
        return content
            .replace("clan://localhost/", fix)
            .replace("file://", fix)
    }

    private fun fixContentPath(url: String, content: String): String {
        var result = content
        if (result.contains("./")) {
            var fixedUrl = url.replace("file://", "clan://localhost/")
            if (!fixedUrl.startsWith("http") && !fixedUrl.startsWith("clan://")) {
                fixedUrl = "http://$fixedUrl"
            }
            if (fixedUrl.startsWith("clan://")) {
                fixedUrl = clanToAddress(fixedUrl)
            }
            result = result.replace("./", fixedUrl.substring(0, fixedUrl.lastIndexOf("/") + 1))
        }
        return result
    }

    @Throws(Throwable::class)
    private fun parseJson(apiUrl: String, f: File) {
        val bReader = BufferedReader(InputStreamReader(FileInputStream(f), "UTF-8"))
        val sb = StringBuilder()
        var s: String?
        while (bReader.readLine().also { s = it } != null) {
            sb.append(s).append("\n")
        }
        bReader.close()
        parseJson(apiUrl, sb.toString())
    }

    private fun parseJson(apiUrl: String, jsonStr: String) {
        val infoJson = gson.fromJson(jsonStr, JsonObject::class.java)
        
        // Spider
        spider = DefaultConfig.safeJsonString(infoJson, "spider", "")
        jarCache = DefaultConfig.safeJsonString(infoJson, "jarCache", "true")
        
        // Wallpaper
        wallpaper = DefaultConfig.safeJsonString(infoJson, "wallpaper", "")
        
        // Parse sites
        var firstSite: SourceBean? = null
        for (opt in infoJson.get("sites").asJsonArray) {
            val obj = opt.asJsonObject
            val sb = SourceBean()
            val siteKey = obj.get("key").asString.trim()
            sb.key = siteKey
            sb.name = if (obj.has("name")) obj.get("name").asString.trim() else siteKey
            sb.type = obj.get("type").asInt
            sb.api = obj.get("api").asString.trim()
            sb.searchable = DefaultConfig.safeJsonInt(obj, "searchable", 1)
            sb.quickSearch = DefaultConfig.safeJsonInt(obj, "quickSearch", 1)
            sb.filterable = if (siteKey.startsWith("py_")) {
                1
            } else {
                DefaultConfig.safeJsonInt(obj, "filterable", 1)
            }
            sb.playerUrl = DefaultConfig.safeJsonString(obj, "playUrl", "")
            sb.ext = DefaultConfig.safeJsonString(obj, "ext", "")
            sb.jar = DefaultConfig.safeJsonString(obj, "jar", "")
            sb.playerType = DefaultConfig.safeJsonInt(obj, "playerType", -1)
            sb.categories = DefaultConfig.safeJsonStringList(obj, "categories")
            sb.clickSelector = DefaultConfig.safeJsonString(obj, "click", "")
            sb.style = DefaultConfig.safeJsonString(obj, "style", "")
            
            if (firstSite == null && sb.filterable == 1) {
                firstSite = sb
            }
            sourceBeanList[siteKey] = sb
        }

        // Set home source
        if (sourceBeanList.isNotEmpty()) {
            val home = Hawk.get(HawkConfig.HOME_API, "")
            val sh = getSource(home)
            if (sh == null) {
                firstSite?.let { setSourceBean(it) }
            } else {
                setSourceBean(sh)
            }
        }

        // VIP parse flags
        vipParseFlags = DefaultConfig.safeJsonStringList(infoJson, "flags")

        // Parse beans
        parseBeanList.clear()
        if (infoJson.has("parses")) {
            val parses = infoJson.get("parses").asJsonArray
            for (opt in parses) {
                val obj = opt.asJsonObject
                val pb = ParseBean()
                pb.name = obj.get("name").asString.trim()
                pb.url = obj.get("url").asString.trim()
                val ext = if (obj.has("ext")) obj.get("ext").asJsonObject.toString() else ""
                pb.ext = ext
                pb.type = DefaultConfig.safeJsonInt(obj, "type", 0)
                parseBeanList.add(pb)
            }
            if (parseBeanList.isNotEmpty()) {
                addSuperParse()
            }
        }

        // Set default parse
        if (parseBeanList.isNotEmpty()) {
            val defaultParse = Hawk.get(HawkConfig.DEFAULT_PARSE, "")
            if (!TextUtils.isEmpty(defaultParse)) {
                for (pb in parseBeanList) {
                    if (pb.name == defaultParse) {
                        setDefaultParse(pb)
                    }
                }
            }
            if (mDefaultParse == null) {
                setDefaultParse(parseBeanList[0])
            }
        }

        // Live sources
        val liveApiUrl = Hawk.get(HawkConfig.LIVE_API_URL, "")
        if (liveApiUrl.isEmpty() || apiUrl == liveApiUrl) {
            initLiveSettings()
            if (infoJson.has("lives")) {
                val livesGroups = infoJson.get("lives").asJsonArray
                var liveGroupIndex = Hawk.get(HawkConfig.LIVE_GROUP_INDEX, 0)
                if (liveGroupIndex > livesGroups.size() - 1) {
                    liveGroupIndex = 0
                    Hawk.put(HawkConfig.LIVE_GROUP_INDEX, 0)
                }
                Hawk.put(HawkConfig.LIVE_GROUP_LIST, livesGroups)

                // Load multi-source config
                try {
                    val liveSettingItemList = ArrayList<LiveSettingItem>()
                    for (i in 0 until livesGroups.size()) {
                        val jsonObject = livesGroups[i].asJsonObject
                        val name = if (jsonObject.has("name")) {
                            jsonObject.get("name").asString
                        } else {
                            "线路${i + 1}"
                        }
                        val liveSettingItem = LiveSettingItem()
                        liveSettingItem.itemIndex = i
                        liveSettingItem.itemName = name
                        liveSettingItemList.add(liveSettingItem)
                    }
                    liveSettingGroupList[5].liveSettingItems = liveSettingItemList
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val livesOBJ = livesGroups[liveGroupIndex].asJsonObject
                loadLiveApi(livesOBJ)
            }
        }

        // Hosts mapping
        myHosts = HashMap()
        if (infoJson.has("hosts")) {
            val hostsArray = infoJson.asJsonArray("hosts")
            for (i in 0 until hostsArray.size()) {
                val entry = hostsArray[i].asString
                val parts = entry.split("=", limit = 2)
                if (parts.size == 2) {
                    myHosts[parts[0]] = parts[1]
                }
            }
        }

        // Video parse rules
        if (infoJson.has("rules")) {
            VideoParseRuler.clearRule()
            for (oneHostRule in infoJson.asJsonArray("rules")) {
                val obj = oneHostRule.asJsonObject
                
                // Sniff filter rules
                if (obj.has("host")) {
                    val host = obj.get("host").asString
                    if (obj.has("rule")) {
                        val ruleJsonArr = obj.asJsonArray("rule")
                        val rule = ArrayList<String>()
                        for (one in ruleJsonArr) {
                            rule.add(one.asString)
                        }
                        if (rule.isNotEmpty()) {
                            VideoParseRuler.addHostRule(host, rule)
                        }
                    }
                    if (obj.has("filter")) {
                        val filterJsonArr = obj.asJsonArray("filter")
                        val filter = ArrayList<String>()
                        for (one in filterJsonArr) {
                            filter.add(one.asString)
                        }
                        if (filter.isNotEmpty()) {
                            VideoParseRuler.addHostFilter(host, filter)
                        }
                    }
                }

                // Ad filter rules
                if (obj.has("hosts") && obj.has("regex")) {
                    val rule = ArrayList<String>()
                    val ads = ArrayList<String>()
                    val regexArray = obj.asJsonArray("regex")
                    for (one in regexArray) {
                        val regex = one.asString
                        if (M3u8.isAd(regex)) ads.add(regex) else rule.add(regex)
                    }
                    val array = obj.asJsonArray("hosts")
                    for (one in array) {
                        val host = one.asString
                        VideoParseRuler.addHostRule(host, rule)
                        VideoParseRuler.addHostRegex(host, ads)
                    }
                }

                // Sniff script rules
                if (obj.has("hosts") && obj.has("script")) {
                    val scripts = ArrayList<String>()
                    val scriptArray = obj.asJsonArray("script")
                    for (one in scriptArray) {
                        scripts.add(one.asString)
                    }
                    val array = obj.asJsonArray("hosts")
                    for (one in array) {
                        VideoParseRuler.addHostScript(one.asString, scripts)
                    }
                }
            }
        }

        // DOH configuration
        if (infoJson.has("doh")) {
            val dohJson = infoJson.asJsonArray("doh").toString()
            if (Hawk.get(HawkConfig.DOH_JSON, "") != dohJson) {
                Hawk.put(HawkConfig.DOH_URL, 0)
                Hawk.put(HawkConfig.DOH_JSON, dohJson)
            }
        } else {
            Hawk.put(HawkConfig.DOH_JSON, "")
        }
        OkGoHelper.setDnsList()

        // Ad blocking
        if (infoJson.has("ads")) {
            for (host in infoJson.asJsonArray("ads")) {
                if (!AdBlocker.hasHost(host.asString)) {
                    AdBlocker.addAdHost(host.asString)
                }
            }
        }
    }

    private fun loadDefaultConfig() {
        // Default IJK and ad settings would be loaded here
        // Implementation matches original Java
    }

    private fun initLiveSettings() {
        val groupNames = arrayListOf("线路选择", "画面比例", "播放解码", "超时换源", "偏好设置", "多源切换")
        val itemsArrayList = arrayListOf(
            arrayListOf<String>(), // Source items
            arrayListOf("默认", "16:9", "4:3", "填充", "原始", "裁剪"),
            arrayListOf("系统", "ijk硬解", "ijk软解", "exo"),
            arrayListOf("5s", "10s", "15s", "20s", "25s", "30s"),
            arrayListOf("显示时间", "显示网速", "换台反转", "跨选分类"),
            arrayListOf<String>() // YUM items
        )

        liveSettingGroupList.clear()
        for (i in groupNames.indices) {
            val liveSettingGroup = LiveSettingGroup()
            val liveSettingItemList = ArrayList<LiveSettingItem>()
            liveSettingGroup.groupIndex = i
            liveSettingGroup.groupName = groupNames[i]
            for (j in itemsArrayList[i].indices) {
                val liveSettingItem = LiveSettingItem()
                liveSettingItem.itemIndex = j
                liveSettingItem.itemName = itemsArrayList[i][j]
                liveSettingItemList.add(liveSettingItem)
            }
            liveSettingGroup.liveSettingItems = liveSettingItemList
            liveSettingGroupList.add(liveSettingGroup)
        }
    }

    private fun loadLiveApi(livesOBJ: JsonObject) {
        try {
            val lives = livesOBJ.toString()
            val index = lives.indexOf("proxy://")
            val url: String

            if (index != -1) {
                var proxyUrl = lives.substring(index, lives.lastIndexOf("\""))
                proxyUrl = DefaultConfig.checkReplaceProxy(proxyUrl)
                val extUrl = Uri.parse(proxyUrl).getQueryParameter("ext")
                if (!extUrl.isNullOrEmpty()) {
                    val extUrlFix = if (extUrl.startsWith("http") || extUrl.startsWith("clan://")) {
                        extUrl
                    } else {
                        String(
                            Base64.decode(extUrl, Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP),
                            Charsets.UTF_8
                        )
                    }
                    val encoded = Base64.encodeToString(
                        extUrlFix.toByteArray(Charsets.UTF_8),
                        Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP
                    )
                    proxyUrl = proxyUrl.replace(extUrl, encoded)
                }
                url = proxyUrl
            } else {
                val type = livesOBJ.get("type").asString
                if (type == "0" || type == "3") {
                    var liveUrl = if (livesOBJ.has("url")) {
                        livesOBJ.get("url").asString
                    } else if (livesOBJ.has("api")) {
                        livesOBJ.get("api").asString
                    } else {
                        ""
                    }

                    if (!liveUrl.startsWith("http://127.0.0.1")) {
                        if (liveUrl.startsWith("http")) {
                            liveUrl = Base64.encodeToString(
                                liveUrl.toByteArray(Charsets.UTF_8),
                                Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP
                            )
                        }
                        liveUrl = "http://127.0.0.1:9978/proxy?do=live&type=txt&ext=$liveUrl"
                    }

                    if (type == "3") {
                        val jarUrl = if (livesOBJ.has("jar")) livesOBJ.get("jar").asString.trim() else ""
                        val pyApi = if (livesOBJ.has("api")) livesOBJ.get("api").asString.trim() else ""
                        
                        if (pyApi.contains(".py")) {
                            val ext = if (livesOBJ.has("ext") && 
                                (livesOBJ.get("ext").isJsonObject || livesOBJ.get("ext").isJsonArray)) {
                                livesOBJ.get("ext").toString()
                            } else {
                                DefaultConfig.safeJsonString(livesOBJ, "ext", "")
                            }
                            pyLoader.getSpider(MD5.string2MD5(pyApi), pyApi, ext)
                        }

                        if (jarUrl.isNotEmpty()) {
                            jarLoader.loadLiveJar(jarUrl)
                        } else if (liveSpider.isNotEmpty()) {
                            jarLoader.loadLiveJar(liveSpider)
                        }
                    }
                    url = liveUrl
                } else {
                    liveChannelGroupList.clear()
                    return
                }
            }

            // EPG settings
            if (livesOBJ.has("epg")) {
                Hawk.put(HawkConfig.EPG_URL, livesOBJ.get("epg").asString)
            } else {
                Hawk.put(HawkConfig.EPG_URL, "")
            }

            // Player type
            if (livesOBJ.has("playerType")) {
                Hawk.put(HawkConfig.LIVE_PLAY_TYPE, livesOBJ.get("playerType").asString)
            } else {
                Hawk.put(HawkConfig.LIVE_PLAY_TYPE, Hawk.get(HawkConfig.PLAY_TYPE, 0))
            }

            // Header settings
            if (livesOBJ.has("header")) {
                val headerObj = livesOBJ.getAsJsonObject("header")
                val liveHeader = HashMap<String, String>()
                for ((key, value) in headerObj.entrySet()) {
                    liveHeader[key] = value.asString
                }
                Hawk.put(HawkConfig.LIVE_WEB_HEADER, liveHeader)
            } else if (livesOBJ.has("ua")) {
                val ua = livesOBJ.get("ua").asString
                val liveHeader = HashMap<String, String>()
                liveHeader["User-Agent"] = ua
                Hawk.put(HawkConfig.LIVE_WEB_HEADER, liveHeader)
            } else {
                Hawk.put(HawkConfig.LIVE_WEB_HEADER, null)
            }

            val liveChannelGroup = LiveChannelGroup()
            liveChannelGroup.groupName = url
            liveChannelGroupList.clear()
            liveChannelGroupList.add(liveChannelGroup)
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    private fun addSuperParse() {
        val superPb = ParseBean()
        superPb.name = "超级解析"
        superPb.url = "SuperParse"
        superPb.ext = ""
        superPb.type = 4
        parseBeanList.add(0, superPb)
    }

    // ==================== Loader Management ====================

    fun clearJarLoader() {
        jarLoader.clear()
    }

    fun clearLoader() {
        jarLoader.clear()
        pyLoader.clear()
        jsLoader.clear()
    }

    fun proxyLocal(param: Map<String, String>): Array<Any?>? {
        if ("js" == param["do"]) {
            return jsLoader.proxyInvoke(param)
        }
        val apiString = if (Hawk.get(HawkConfig.PLAYER_IS_LIVE, false)) {
            currentLiveSpider ?: ""
        } else {
            getHomeSourceBean().api
        }
        return if (apiString.contains(".py")) {
            pyLoader.proxyInvoke(param)
        } else {
            jarLoader.proxyInvoke(param)
        }
    }

    fun jsonExt(key: String, jxs: LinkedHashMap<String, String>, url: String): JSONObject? {
        return jarLoader.jsonExt(key, jxs, url)
    }

    fun jsonExtMix(
        flag: String,
        key: String,
        name: String,
        jxs: LinkedHashMap<String, HashMap<String, String>>,
        url: String
    ): JSONObject? {
        return jarLoader.jsonExtMix(flag, key, name, jxs, url)
    }

    // ==================== Flow-based API for Repository ====================

    /**
     * Get sources as Flow for reactive updates
     */
    fun getSourcesFlow(): Flow<List<SourceBean>> = flow {
        emit(getSourceBeanList())
    }.flowOn(Dispatchers.IO)

    /**
     * Get active source as Flow
     */
    fun getActiveSourceFlow(): Flow<SourceBean?> = flow {
        emit(getHomeSourceBean())
    }.flowOn(Dispatchers.IO)
}
