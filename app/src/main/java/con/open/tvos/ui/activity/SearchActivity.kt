package con.open.tvos.ui.activity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.chad.library.adapter.base.BaseQuickAdapter
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.BaseActivity
import con.open.tvos.bean.AbsXml
import con.open.tvos.bean.Movie
import con.open.tvos.bean.SourceBean
import con.open.tvos.crawler.JsLoader
import con.open.tvos.event.RefreshEvent
import con.open.tvos.event.ServerEvent
import con.open.tvos.ui.adapter.PinyinAdapter
import con.open.tvos.ui.adapter.SearchAdapter
import con.open.tvos.ui.dialog.RemoteDialog
import con.open.tvos.ui.dialog.SearchCheckboxDialog
import con.open.tvos.ui.tv.widget.SearchKeyboard
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.HawkConfig
import con.open.tvos.util.HistoryHelper
import con.open.tvos.util.SearchHelper
import con.open.tvos.viewmodel.SourceViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
class SearchActivity : BaseActivity() {
    private lateinit var llLayout: LinearLayout
    private lateinit var mGridView: TvRecyclerView
    private lateinit var mGridViewWord: TvRecyclerView
    lateinit var sourceViewModel: SourceViewModel
    private var remoteDialog: RemoteDialog? = null
    private lateinit var etSearch: android.widget.EditText
    private lateinit var tvSearch: TextView
    private lateinit var tvClear: TextView
    private lateinit var keyboard: SearchKeyboard
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var wordAdapter: PinyinAdapter
    private var searchTitle = ""
    private lateinit var tvSearchCheckboxBtn: TextView

    private var mCheckSources: HashMap<String, String>? = null
    private var mSearchCheckboxDialog: SearchCheckboxDialog? = null

    private lateinit var wordsSwitch: TextView

    override fun getLayoutResID(): Int = R.layout.activity_search

    companion object {
        private var hasKeyBoard: Boolean? = null
        private var isSearchBack: Boolean? = null
        private var hots: ArrayList<String>? = null
        private var mCheckSourcesStatic: HashMap<String, String>? = null

        fun setCheckedSourcesForSearch(checkedSources: HashMap<String, String>) {
            mCheckSourcesStatic = checkedSources
        }
    }

    override fun init() {
        initView()
        initViewModel()
        initData()
        hasKeyBoard = true
        isSearchBack = false
    }

    private var pauseRunnable: List<Runnable>? = null

    override fun onResume() {
        super.onResume()
        pauseRunnable?.let { runnables ->
            if (runnables.isNotEmpty()) {
                searchExecutorService = Executors.newFixedThreadPool(5)
                allRunCount.set(runnables.size)
                runnables.forEach { runnable ->
                    searchExecutorService?.execute(runnable)
                }
                pauseRunnable = null
            }
        }
        when {
            hasKeyBoard == true -> {
                tvSearch.requestFocus()
                tvSearch.requestFocusFromTouch()
            }
            isSearchBack != true -> {
                etSearch.requestFocus()
                etSearch.requestFocusFromTouch()
            }
        }
    }

    private fun initView() {
        EventBus.getDefault().register(this)
        llLayout = findViewById(R.id.llLayout)
        etSearch = findViewById(R.id.etSearch)
        tvSearch = findViewById(R.id.tvSearch)
        tvSearchCheckboxBtn = findViewById(R.id.tvSearchCheckboxBtn)
        tvClear = findViewById(R.id.tvClear)
        mGridView = findViewById(R.id.mGridView)
        keyboard = findViewById(R.id.keyBoardRoot)
        mGridViewWord = findViewById(R.id.mGridViewWord)
        mGridViewWord.setHasFixedSize(true)
        mGridViewWord.layoutManager = V7LinearLayoutManager(this.mContext, 1, false)
        wordAdapter = PinyinAdapter()
        mGridViewWord.adapter = wordAdapter
        wordsSwitch = findViewById(R.id.wordSwitch)
        
        wordAdapter.setOnItemClickListener { adapter, _, position ->
            if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                val bundle = Bundle().apply {
                    putString("title", adapter.getItem(position) as String)
                }
                jumpActivity(FastSearchActivity::class.java, bundle)
            } else {
                search(adapter.getItem(position) as String)
            }
        }
        
        mGridView.setHasFixedSize(true)
        // lite
        if (Hawk.get(HawkConfig.SEARCH_VIEW, 0) == 0) {
            mGridView.layoutManager = V7LinearLayoutManager(this.mContext, 1, false)
        } else {
            // with preview
            mGridView.layoutManager = V7GridLayoutManager(this.mContext, 3)
        }
        searchAdapter = SearchAdapter()
        mGridView.adapter = searchAdapter
        
        searchAdapter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            val video = searchAdapter.data[position]
            video?.let {
                try {
                    searchExecutorService?.let { service ->
                        pauseRunnable = service.shutdownNow()
                        searchExecutorService = null
                        JsLoader.stopAll()
                    }
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
                hasKeyBoard = false
                isSearchBack = true
                val bundle = Bundle().apply {
                    putString("id", it.id)
                    putString("sourceKey", it.sourceKey)
                }
                jumpActivity(DetailActivity::class.java, bundle)
            }
        }
        
        wordsSwitch.setOnClickListener { v ->
            FastClickCheckUtil.check(v)
            val wd = wordsSwitch.text.toString().trim()
            when {
                wd.contains("热词") -> {
                    val hisWord: ArrayList<String> = Hawk.get(HawkConfig.SEARCH_HISTORY, ArrayList())
                    if (hisWord.isEmpty()) {
                        Toast.makeText(mContext, "暂无历史搜索", Toast.LENGTH_SHORT).show()
                    } else {
                        wordsSwitch.text = "历史 搜索"
                        wordAdapter.setNewData(hisWord)
                    }
                }
                wd == "历史 搜索" -> {
                    wordsSwitch.text = "热词 搜索"
                    hots?.takeIf { it.isNotEmpty() }?.let {
                        wordAdapter.setNewData(it)
                    }
                }
            }
        }
        
        tvSearch.setOnClickListener { v ->
            FastClickCheckUtil.check(v)
            hasKeyBoard = true
            val wd = etSearch.text.toString().trim()
            if (!TextUtils.isEmpty(wd)) {
                if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                    val bundle = Bundle().apply {
                        putString("title", wd)
                    }
                    jumpActivity(FastSearchActivity::class.java, bundle)
                } else {
                    search(wd)
                }
            } else {
                Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show()
            }
        }
        
        tvClear.setOnClickListener { v ->
            FastClickCheckUtil.check(v)
            initData()
            etSearch.setText("")
        }

        //软键盘
        etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                val wd = etSearch.text.toString().trim()
                if (!TextUtils.isEmpty(wd)) {
                    if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                        val bundle = Bundle().apply {
                            putString("title", wd)
                        }
                        jumpActivity(FastSearchActivity::class.java, bundle)
                    } else {
                        hiddenImm()
                        search(wd)
                    }
                } else {
                    Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        // 监听遥控器
        etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && 
                (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                val wd = etSearch.text.toString().trim()
                if (!TextUtils.isEmpty(wd)) {
                    if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                        val bundle = Bundle().apply {
                            putString("title", wd)
                        }
                        jumpActivity(FastSearchActivity::class.java, bundle)
                    } else {
                        hiddenImm()
                        search(wd)
                    }
                } else {
                    Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
        
        keyboard.setOnSearchKeyListener(object : SearchKeyboard.OnSearchKeyListener {
            override fun onSearchKey(pos: Int, key: String) {
                when {
                    pos > 1 -> {
                        val text = etSearch.text.toString().trim() + key
                        etSearch.setText(text)
                        if (text.isNotEmpty()) {
                            loadRec(text)
                        }
                    }
                    pos == 1 -> {
                        var text = etSearch.text.toString().trim()
                        if (text.isNotEmpty()) {
                            text = text.substring(0, text.length - 1)
                            etSearch.setText(text)
                        }
                        if (text.isNotEmpty()) {
                            loadRec(text)
                        }
                    }
                    pos == 0 -> {
                        remoteDialog = RemoteDialog(mContext)
                        remoteDialog?.show()
                    }
                }
            }
        })
        
        setLoadSir(llLayout)
        
        tvSearchCheckboxBtn.setOnClickListener {
            val searchAbleSource = ApiConfig.get().searchSourceBeanList
            if (mSearchCheckboxDialog == null) {
                mSearchCheckboxDialog = SearchCheckboxDialog(this@SearchActivity, searchAbleSource, mCheckSources)
            } else {
                if (searchAbleSource.size != mSearchCheckboxDialog?.mSourceList?.size) {
                    mSearchCheckboxDialog?.setMSourceList(searchAbleSource)
                }
            }
            mSearchCheckboxDialog?.setOnDismissListener { dialog ->
                dialog.dismiss()
            }
            mSearchCheckboxDialog?.show()
        }
    }

    private fun initViewModel() {
        sourceViewModel = ViewModelProvider(this)[SourceViewModel::class.java]
    }

    /**
     * 拼音联想
     */
    private fun loadRec(key: String) {
        OkGo.get<String>("https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box")
            .params("format", "json")
            .params("page_num", 0)
            .params("page_size", 20)
            .params("key", key)
            .execute(object : AbsCallback<String>() {
                override fun onSuccess(response: Response<String>) {
                    try {
                        val hotsList = ArrayList<String>()
                        val result = response.body()
                        val gson = Gson()
                        val json = gson.fromJson(result, JsonElement::class.java)
                        val groupDataArr = json.asJsonObject
                            .get("data").asJsonObject
                            .get("search_data").asJsonObject
                            .get("vecGroupData").asJsonArray
                            .get(0).asJsonObject
                            .get("group_data").asJsonArray
                        for (groupDataElement in groupDataArr) {
                            val groupData = groupDataElement.asJsonObject
                            val keywordTxt = groupData.getAsJsonObject("dtReportInfo")
                                .getAsJsonObject("reportData")
                                .get("keyword_txt").asString
                            hotsList.add(keywordTxt.trim())
                        }
                        wordsSwitch.text = "猜你 想搜"
                        wordAdapter.setNewData(hotsList)
                        mGridViewWord.smoothScrollToPosition(0)
                    } catch (th: Throwable) {
                        th.printStackTrace()
                    }
                }

                override fun convertResponse(response: okhttp3.Response): String {
                    return response.body?.string() ?: ""
                }
            })
    }

    private fun initData() {
        initCheckedSourcesForSearch()
        intent?.let { intent ->
            if (intent.hasExtra("title")) {
                val title = intent.getStringExtra("title")
                title?.let {
                    showLoading()
                    if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                        val bundle = Bundle().apply {
                            putString("title", it)
                        }
                        jumpActivity(FastSearchActivity::class.java, bundle)
                    } else {
                        search(it)
                    }
                }
            }
        }
        wordsSwitch.text = "热词 | 历史"
        hots?.takeIf { it.isNotEmpty() }?.let {
            wordAdapter.setNewData(it)
            return
        }
        // 加载热词
        OkGo.get<String>("https://node.video.qq.com/x/api/hot_search")
            .params("channdlId", "0")
            .params("_", System.currentTimeMillis())
            .execute(object : AbsCallback<String>() {
                override fun onSuccess(response: Response<String>) {
                    try {
                        hots = ArrayList()
                        val itemList = JsonParser.parseString(response.body())
                            .asJsonObject.get("data").asJsonObject
                            .get("mapResult").asJsonObject
                            .get("0").asJsonObject
                            .get("listInfo").asJsonArray
                        for (ele in itemList) {
                            val obj = ele.asJsonObject
                            hots?.add(
                                obj.get("title").asString.trim()
                                    .replaceAll("<|>|《|》|-".toRegex(), "")
                                    .split(" ")[0]
                            )
                        }
                        hots?.let { wordAdapter.setNewData(it) }
                    } catch (th: Throwable) {
                        th.printStackTrace()
                    }
                }

                override fun convertResponse(response: okhttp3.Response): String {
                    return response.body?.string() ?: ""
                }
            })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun server(event: ServerEvent) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            val title = event.obj as String
            showLoading()
            if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                val bundle = Bundle().apply {
                    putString("title", title)
                }
                jumpActivity(FastSearchActivity::class.java, bundle)
            } else {
                search(title)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refresh(event: RefreshEvent) {
        if (event.type == RefreshEvent.TYPE_SEARCH_RESULT) {
            try {
                searchData(if (event.obj == null) null else event.obj as AbsXml)
            } catch (e: Exception) {
                searchData(null)
            }
        }
    }

    private fun initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch()
    }

    private fun search(title: String) {
        cancel()
        remoteDialog?.dismiss()
        remoteDialog = null
        showLoading()
        etSearch.setText(title)

        //写入历史记录
        HistoryHelper.setSearchHistory(title)

        this.searchTitle = title
        mGridView.visibility = View.INVISIBLE
        searchAdapter.setNewData(ArrayList())
        searchResult()
    }

    private var searchExecutorService: ExecutorService? = null
    private var allRunCount = AtomicInteger(0)

    private fun searchResult() {
        try {
            searchExecutorService?.let { service ->
                service.shutdownNow()
                searchExecutorService = null
                JsLoader.stopAll()
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        } finally {
            searchAdapter.setNewData(ArrayList())
            allRunCount.set(0)
        }
        
        searchExecutorService = Executors.newFixedThreadPool(5)
        val searchRequestList = ArrayList<SourceBean>().apply {
            addAll(ApiConfig.get().sourceBeanList)
        }
        val home = ApiConfig.get().homeSourceBean
        searchRequestList.remove(home)
        searchRequestList.add(0, home)

        val siteKey = ArrayList<String>()
        for (bean in searchRequestList) {
            if (!bean.isSearchable) {
                continue
            }
            if (mCheckSources != null && !mCheckSources!!.containsKey(bean.key)) {
                continue
            }
            siteKey.add(bean.key)
            allRunCount.incrementAndGet()
        }
        
        if (siteKey.isEmpty()) {
            Toast.makeText(mContext, "没有指定搜索源", Toast.LENGTH_SHORT).show()
            showEmpty()
            return
        }
        
        for (key in siteKey) {
            searchExecutorService?.execute {
                sourceViewModel.getSearch(key, searchTitle)
            }
        }
    }

    private fun matchSearchResult(name: String?, searchTitle: String?): Boolean {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(searchTitle)) return false
        val trimmedSearchTitle = searchTitle!!.trim()
        val arr = trimmedSearchTitle.split("\\s+".toRegex())
        var matchNum = 0
        for (one in arr) {
            if (name!!.contains(one)) matchNum++
        }
        return matchNum == arr.size
    }

    private fun searchData(absXml: AbsXml?) {
        if (absXml?.movie?.videoList != null && absXml.movie.videoList.isNotEmpty()) {
            val data = ArrayList<Movie.Video>()
            for (video in absXml.movie.videoList) {
                if (matchSearchResult(video.name, searchTitle)) {
                    data.add(video)
                }
            }
            if (searchAdapter.data.isNotEmpty()) {
                searchAdapter.addData(data)
            } else {
                showSuccess()
                mGridView.visibility = View.VISIBLE
                searchAdapter.setNewData(data)
            }
        }

        val count = allRunCount.decrementAndGet()
        if (count <= 0) {
            if (searchAdapter.data.isEmpty()) {
                showEmpty()
            }
            cancel()
        }
    }

    private fun cancel() {
        OkGo.getInstance().cancelTag("search")
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
        try {
            searchExecutorService?.let { service ->
                service.shutdownNow()
                searchExecutorService = null
                JsLoader.stopAll()
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        EventBus.getDefault().unregister(this)
    }

    private fun hiddenImm() {
        val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }
}
