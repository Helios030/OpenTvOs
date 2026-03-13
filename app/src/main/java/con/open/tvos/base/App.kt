package con.open.tvos.base

import android.app.Activity
import androidx.multidex.MultiDexApplication
import con.open.tvos.bean.VodInfo
import con.open.tvos.callback.EmptyCallback
import con.open.tvos.callback.LoadingCallback
import con.open.tvos.data.AppDataManager
import con.open.tvos.server.ControlManager
import con.open.tvos.util.AppManager
import con.open.tvos.util.EpgUtil
import con.open.tvos.util.FileUtils
import con.open.tvos.util.HawkConfig
import con.open.tvos.util.LOG
import con.open.tvos.util.OkGoHelper
import con.open.tvos.util.PlayerHelper
import com.kingja.loadsir.core.LoadSir
import com.orhanobut.hawk.Hawk
import com.p2p.P2PClass
import com.whl.quickjs.android.QuickJSLoader
import con.open.tvos.crawler.JsLoader
import dagger.hilt.android.HiltAndroidApp
import me.jessyan.autosize.AutoSizeConfig
import me.jessyan.autosize.unit.Subunits

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
@HiltAndroidApp
class App : MultiDexApplication() {

    private var vodInfo: VodInfo? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        initParams()
        // OKGo
        OkGoHelper.init()
        // 台标获取
        EpgUtil.init()
        // 初始化Web服务器
        ControlManager.init(this)
        // 初始化数据库
        AppDataManager.init()
        LoadSir.beginBuilder()
            .addCallback(EmptyCallback())
            .addCallback(LoadingCallback())
            .commit()
        AutoSizeConfig.getInstance().setCustomFragment(true).unitsManager
            .setSupportDP(false)
            .setSupportSP(false)
            .setSupportSubunits(Subunits.MM)
        PlayerHelper.init()
        QuickJSLoader.init()
        FileUtils.cleanPlayerCache()
    }

    private fun initParams() {
        // Hawk
        Hawk.init(this).build()
        Hawk.put(HawkConfig.DEBUG_OPEN, false)
        if (!Hawk.contains(HawkConfig.PLAY_TYPE)) {
            Hawk.put(HawkConfig.PLAY_TYPE, 1)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        JsLoader.destroy()
    }

    fun setVodInfo(vodInfo: VodInfo?) {
        this.vodInfo = vodInfo
    }

    fun getVodInfo(): VodInfo? {
        return vodInfo
    }

    fun getCurrentActivity(): Activity? {
        return AppManager.getInstance().currentActivity()
    }

    fun setDashData(data: String?) {
        dashData = data
    }

    fun getDashData(): String? {
        return dashData
    }

    companion object {
        @Volatile
        private lateinit var instance: App

        private var p: P2PClass? = null

        @JvmField
        var burl: String? = null

        private var dashData: String? = null

        @JvmStatic
        fun getInstance(): App {
            return instance
        }

        @JvmStatic
        fun getp2p(): P2PClass? {
            return try {
                if (p == null) {
                    p = P2PClass(FileUtils.getExternalCachePath())
                }
                p
            } catch (e: Exception) {
                LOG.e(e.toString())
                null
            }
        }
    }
}
