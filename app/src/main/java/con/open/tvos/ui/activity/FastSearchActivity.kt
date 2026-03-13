package con.open.tvos.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import con.open.tvos.crawler.JsLoader
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.BaseActivity
import con.open.tvos.bean.AbsXml
import con.open.tvos.bean.Movie
import con.open.tvos.bean.SourceBean
import con.open.tvos.event.RefreshEvent
import con.open.tvos.event.ServerEvent
import con.open.tvos.ui.adapter.FastListAdapter
import con.open.tvos.ui.adapter.FastSearchAdapter
import con.open.tvos.ui.adapter.SearchWordAdapter
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.HistoryHelper
import con.open.tvos.util.SearchHelper
import con.open.tvos.viewmodel.SourceViewModel
import com.lzy.okgo.OkGo
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
class FastSearchActivity : BaseActivity() {
    private lateinit var llLayout: LinearLayout
    private lateinit var mSearchTitle: TextView
    private lateinit var mGridView: TvRecyclerView
    private lateinit var mGridViewFilter: TvRecyclerView
    private lateinit var mGridViewWord: TvRecyclerView
    private lateinit var mGridViewWordFenci: TvRecyclerView
    lateinit var sourceViewModel: SourceViewModel

    private lateinit var searchWordAdapter: SearchWordAdapter
    private lateinit var searchAdapter: FastSearchAdapter
    private lateinit var searchAdapterFilter: FastSearchAdapter
    private lateinit var spListAdapter: FastListAdapter
    private var searchTitle = ""
    private lateinit var spNames: HashMap<String, String>
    private var isFilterMode = false
    private var searchFilterKey = "" // 过滤的key
    private lateinit var resultVods: HashMap<String, ArrayList<Movie.Video>> // 搜索结果
    private val quickSearchWord = ArrayList<String>()
    private var mCheckSources: HashMap<String, String>? = null

    private val focusChangeListener = View.OnFocusChangeListener { itemView, hasFocus ->
        try {
            if (!hasFocus) {
                spListAdapter.onLostFocus(itemView)
            } else {
                val ret = spListAdapter.onSetFocus(itemView)
                if (ret < 0) return@OnFocusChangeListener
                val v = itemView as TextView
                val sb = v.text.toString()
                filterResult(sb)
            }
        } catch (e: Exception) {
            Toast.makeText(this@FastSearchActivity, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    override fun getLayoutResID(): Int {
        return R.layout.activity_fast_search
    }

    override fun init() {
        spNames = HashMap()
        resultVods = HashMap()
        initView()
        initViewModel()
        initData()
    }

    private var pauseRunnable: List<Runnable>? = null

    override fun onResume() {
        super.onResume()
        pauseRunnable?.let { runnableList ->
            if (runnableList.isNotEmpty()) {
                searchExecutorService = Executors.newFixedThreadPool(5)
                allRunCount.set(runnableList.size)
                for (runnable in runnableList) {
                    searchExecutorService?.execute(runnable)
                }
                pauseRunnable = null
            }
        }
    }

    private fun initView() {
        EventBus.getDefault().register(this)
        llLayout = findViewById(R.id.llLayout)
        mSearchTitle = findViewById(R.id.mSearchTitle)
        mGridView = findViewById(R.id.mGridView)
        mGridViewWord = findViewById(R.id.mGridViewWord)
        mGridViewFilter = findViewById(R.id.mGridViewFilter)

        mGridViewWord.setHasFixedSize(true)
        mGridViewWord.setLayoutManager(V7LinearLayoutManager(this.mContext, 1, false))
        spListAdapter = FastListAdapter()
        mGridViewWord.setAdapter(spListAdapter)

        mGridViewWord.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(@NonNull child: View) {
                child.isFocusable = true
                child.onFocusChangeListener = focusChangeListener
                val t = child as TextView
                if (t.text == "全部") {
                    t.requestFocus()
                }
            }

            override fun onChildViewDetachedFromWindow(@NonNull view: View) {
                view.onFocusChangeListener = null
            }
        })

        spListAdapter.setOnItemClickListener { adapter, view, position ->
            val spName = spListAdapter.getItem(position)
            filterResult(spName)
        }

        mGridView.setHasFixedSize(true)
        mGridView.setLayoutManager(V7GridLayoutManager(this.mContext, 5))

        searchAdapter = FastSearchAdapter()
        mGridView.setAdapter(searchAdapter)

        searchAdapter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            val video = searchAdapter.data[position]
            video?.let {
                try {
                    searchExecutorService?.let { executor ->
                        pauseRunnable = executor.shutdownNow()
                        searchExecutorService = null
                        JsLoader.stopAll()
                    }
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
                val bundle = Bundle()
                bundle.putString("id", it.id)
                bundle.putString("sourceKey", it.sourceKey)
                jumpActivity(DetailActivity::class.java, bundle)
            }
        }

        mGridViewFilter.setLayoutManager(V7GridLayoutManager(this.mContext, 5))
        searchAdapterFilter = FastSearchAdapter()
        mGridViewFilter.setAdapter(searchAdapterFilter)
        searchAdapterFilter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            val video = searchAdapterFilter.data[position]
            video?.let {
                try {
                    searchExecutorService?.let { executor ->
                        pauseRunnable = executor.shutdownNow()
                        searchExecutorService = null
                        JsLoader.stopAll()
                    }
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
                val bundle = Bundle()
                bundle.putString("id", it.id)
                bundle.putString("sourceKey", it.sourceKey)
                jumpActivity(DetailActivity::class.java, bundle)
            }
        }

        setLoadSir(llLayout)

        // 分词
        searchWordAdapter = SearchWordAdapter()
        mGridViewWordFenci = findViewById(R.id.mGridViewWordFenci)
        mGridViewWordFenci.setAdapter(searchWordAdapter)
        mGridViewWordFenci.setLayoutManager(V7LinearLayoutManager(this.mContext, 0, false))
        searchWordAdapter.setOnItemClickListener { _, _, position ->
            val str = searchWordAdapter.data[position]
            search(str)
        }
        searchWordAdapter.setNewData(ArrayList())
    }

    private fun initViewModel() {
        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
    }

    private fun filterResult(spName: String) {
        if (spName == "全部") {
            mGridView.visibility = View.VISIBLE
            mGridViewFilter.visibility = View.GONE
            return
        }
        mGridView.visibility = View.GONE
        mGridViewFilter.visibility = View.VISIBLE
        val key = spNames[spName] ?: return
        if (key.isEmpty()) return

        if (searchFilterKey == key) return
        searchFilterKey = key

        val list = resultVods[key]
        searchAdapterFilter.setNewData(list)
    }

    private fun fenci() {
        if (quickSearchWord.isNotEmpty()) return // 如果经有分词了，不再进行二次分词
        quickSearchWord.addAll(SearchHelper.splitWords(searchTitle))
        val words = ArrayList(HashSet(quickSearchWord))
        EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, words))
    }

    private fun initData() {
        initCheckedSourcesForSearch()
        val intent = intent
        if (intent != null && intent.hasExtra("title")) {
            val title = intent.getStringExtra("title")
            showLoading()
            search(title ?: "")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun server(event: ServerEvent) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            val title = event.obj as String
            showLoading()
            search(title)
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
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_WORD) {
            event.obj?.let {
                val data = it as List<String>
                searchWordAdapter.setNewData(data)
            }
        }
        mSearchTitle?.let {
            it.text = String.format("已搜索( %d )", resultVods.size)
        }
    }

    private fun initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch()
    }

    private fun search(title: String) {
        cancel()
        showLoading()
        this.searchTitle = title
        fenci()
        mGridView.visibility = View.INVISIBLE
        mGridViewFilter.visibility = View.GONE
        searchAdapter.setNewData(ArrayList())
        searchAdapterFilter.setNewData(ArrayList())

        spListAdapter.reset()
        resultVods.clear()
        searchFilterKey = ""
        isFilterMode = false
        spNames.clear()

        //写入历史记录
        HistoryHelper.setSearchHistory(title)

        searchResult()
    }

    private var searchExecutorService: ExecutorService? = null
    private val allRunCount = AtomicInteger(0)

    private fun searchResult() {
        try {
            searchExecutorService?.let { executor ->
                executor.shutdownNow()
                searchExecutorService = null
                JsLoader.stopAll()
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        } finally {
            searchAdapter.setNewData(ArrayList())
            searchAdapterFilter.setNewData(ArrayList())
            allRunCount.set(0)
        }
        searchExecutorService = Executors.newFixedThreadPool(5)
        val searchRequestList = ArrayList<SourceBean>()
        searchRequestList.addAll(ApiConfig.get().sourceBeanList)
        val home = ApiConfig.get().homeSourceBean
        searchRequestList.remove(home)
        searchRequestList.add(0, home)

        val siteKey = ArrayList<String>()
        val hots = ArrayList<String>()

        spListAdapter.setNewData(hots)
        spListAdapter.addData("全部")
        for (bean in searchRequestList) {
            if (!bean.isSearchable) {
                continue
            }
            if (mCheckSources != null && !mCheckSources!!.containsKey(bean.key)) {
                continue
            }
            siteKey.add(bean.key)
            this.spNames[bean.name] = bean.key
            allRunCount.incrementAndGet()
        }

        for (key in siteKey) {
            searchExecutorService?.execute {
                try {
                    sourceViewModel.getSearch(key, searchTitle)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    // 向过滤栏添加有结果的spname
    private fun addWordAdapterIfNeed(key: String): String {
        try {
            var name = ""
            for (n in spNames.keys) {
                if (spNames[n] == key) {
                    name = n
                }
            }
            if (name.isEmpty()) return key

            val names = spListAdapter.data
            for (i in names.indices) {
                if (name == names[i]) {
                    return key
                }
            }

            spListAdapter.addData(name)
            return key
        } catch (e: Exception) {
            return key
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
        var lastSourceKey = ""

        if (absXml != null && absXml.movie != null && absXml.movie!!.videoList != null && absXml.movie!!.videoList!!.isNotEmpty()) {
            val data = ArrayList<Movie.Video>()
            for (video in absXml.movie!!.videoList!!) {
                if (!matchSearchResult(video.name, searchTitle)) continue
                data.add(video)
                if (!resultVods.containsKey(video.sourceKey)) {
                    resultVods[video.sourceKey] = ArrayList()
                }
                resultVods[video.sourceKey]?.add(video)
                if (video.sourceKey != lastSourceKey) {
                    lastSourceKey = this.addWordAdapterIfNeed(video.sourceKey)
                }
            }

            if (searchAdapter.data.isNotEmpty()) {
                searchAdapter.addData(data)
            } else {
                showSuccess()
                if (!isFilterMode)
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
            searchExecutorService?.let { executor ->
                executor.shutdownNow()
                searchExecutorService = null
                JsLoader.stopAll()
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        EventBus.getDefault().unregister(this)
    }
}
