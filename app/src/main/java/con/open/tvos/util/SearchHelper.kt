package con.open.tvos.util

import com.orhanobut.hawk.Hawk
import con.open.tvos.api.ApiConfig
import con.open.tvos.ui.activity.SearchActivity

object SearchHelper {

    @JvmStatic
    fun getSourcesForSearch(): HashMap<String, String>? {
        var mCheckSources: HashMap<String, String>?
        try {
            val api = Hawk.get(HawkConfig.API_URL, "")
            if (api.isEmpty()) return null
            val mCheckSourcesForApi: HashMap<String, HashMap<String, String>> = 
                Hawk.get(HawkConfig.SOURCES_FOR_SEARCH, HashMap())
            mCheckSources = mCheckSourcesForApi[api]
        } catch (e: Exception) {
            return null
        }
        if (mCheckSources.isNullOrEmpty()) {
            mCheckSources = getSources()
        }
        return mCheckSources
    }

    @JvmStatic
    fun putCheckedSources(mCheckSources: HashMap<String, String>?, isAll: Boolean) {
        val api = Hawk.get(HawkConfig.API_URL, "")
        if (api.isEmpty()) return
        
        var mCheckSourcesForApi: HashMap<String, HashMap<String, String>>? = 
            Hawk.get(HawkConfig.SOURCES_FOR_SEARCH, null)

        if (isAll) {
            if (mCheckSourcesForApi == null) return
            mCheckSourcesForApi.remove(api)
        } else {
            if (mCheckSourcesForApi == null) mCheckSourcesForApi = HashMap()
            mCheckSourcesForApi[api] = mCheckSources!!
        }
        SearchActivity.setCheckedSourcesForSearch(mCheckSources)
        Hawk.put(HawkConfig.SOURCES_FOR_SEARCH, mCheckSourcesForApi)
    }

    @JvmStatic
    fun getSources(): HashMap<String, String> {
        val mCheckSources = HashMap<String, String>()
        for (bean in ApiConfig.get().sourceBeanList) {
            if (!bean.isSearchable) continue
            mCheckSources[bean.key] = "1"
        }
        return mCheckSources
    }

    @JvmStatic
    fun splitWords(text: String): List<String> {
        val result = ArrayList<String>()
        result.add(text)
        val parts = text.split("\\W+".toRegex())
        if (parts.size > 1) {
            result.addAll(parts)
        }
        return result
    }
}
