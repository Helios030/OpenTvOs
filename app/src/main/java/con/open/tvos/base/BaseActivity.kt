package con.open.tvos.base

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import con.open.tvos.R
import con.open.tvos.callback.EmptyCallback
import con.open.tvos.callback.LoadingCallback
import con.open.tvos.util.AppManager
import com.kingja.loadsir.callback.Callback
import com.kingja.loadsir.core.LoadService
import com.kingja.loadsir.core.LoadSir
import me.jessyan.autosize.AutoSizeCompat
import me.jessyan.autosize.internal.CustomAdapt
import xyz.doikki.videoplayer.util.CutoutUtil
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
abstract class BaseActivity : AppCompatActivity(), CustomAdapt {

    protected lateinit var mContext: Context
    private var mLoadService: LoadService? = null

    companion object {
        private var screenRatio = -100.0f
        private var globalWp: BitmapDrawable? = null
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        try {
            if (screenRatio < 0) {
                val dm = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(dm)
                val screenWidth = dm.widthPixels
                val screenHeight = dm.heightPixels
                screenRatio = maxOf(screenWidth, screenHeight).toFloat() / minOf(screenWidth, screenHeight)
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResID())
        mContext = this
        CutoutUtil.adaptCutoutAboveAndroidP(mContext, true) //设置刘海
        AppManager.getInstance().addActivity(this)
        init()
    }

    override fun onResume() {
        super.onResume()
        hideSysBar()
        changeWallpaper(false)
    }

    fun hideSysBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            var uiOptions = window.decorView.systemUiVisibility
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_FULLSCREEN
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            window.decorView.systemUiVisibility = uiOptions
        }
    }

    override fun getResources(): Resources {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            AutoSizeCompat.autoConvertDensityOfCustomAdapt(super.getResources(), this)
        }
        return super.getResources()
    }

    fun hasPermission(permission: String): Boolean {
        return try {
            PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

    protected abstract val layoutResID: Int

    protected abstract fun init()

    protected fun setLoadSir(view: View) {
        if (mLoadService == null) {
            mLoadService = LoadSir.getDefault().register(view) {
                // OnReload callback - empty implementation
            }
        }
    }

    protected fun showLoading() {
        mLoadService?.showCallback(LoadingCallback::class.java)
    }

    protected fun isLoading(): Boolean {
        return mLoadService?.currentCallback?.equals(LoadingCallback::class.java) ?: false
    }

    protected fun showEmpty() {
        mLoadService?.showCallback(EmptyCallback::class.java)
    }

    protected fun showSuccess() {
        mLoadService?.showSuccess()
    }

    override fun onDestroy() {
        super.onDestroy()
        AppManager.getInstance().finishActivity(this)
    }

    fun jumpActivity(clazz: Class<out BaseActivity>) {
        val intent = Intent(mContext, clazz)
        startActivity(intent)
    }

    fun jumpActivity(clazz: Class<out BaseActivity>, bundle: Bundle) {
        val intent = Intent(mContext, clazz)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    protected fun getAssetText(fileName: String): String {
        return try {
            val assets: AssetManager = assets
            val bf = BufferedReader(InputStreamReader(assets.open(fileName)))
            buildString {
                var line: String? = bf.readLine()
                while (line != null) {
                    append(line)
                    line = bf.readLine()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    override fun getSizeInDp(): Float {
        return if (isBaseOnWidth) 1280f else 720f
    }

    override fun isBaseOnWidth(): Boolean {
        return screenRatio < 4.0f
    }

    fun changeWallpaper(force: Boolean) {
        if (!force && globalWp != null) {
            window.setBackgroundDrawable(globalWp)
        }
        try {
            val wp = File("${filesDir.absolutePath}/wp")
            if (wp.exists()) {
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(wp.absolutePath, opts)
                // 从Options中获取图片的分辨率
                val imageHeight = opts.outHeight
                val imageWidth = opts.outWidth
                val picHeight = 720
                val picWidth = 1080
                val scaleX = imageWidth / picWidth
                val scaleY = imageHeight / picHeight
                var scale = 1
                if (scaleX > scaleY && scaleY >= 1) {
                    scale = scaleX
                }
                if (scaleX < scaleY && scaleX >= 1) {
                    scale = scaleY
                }
                opts.inJustDecodeBounds = false
                // 采样率
                opts.inSampleSize = scale
                globalWp = BitmapDrawable(BitmapFactory.decodeFile(wp.absolutePath, opts))
            } else {
                globalWp = null
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            globalWp = null
        }
        if (globalWp != null) {
            window.setBackgroundDrawable(globalWp)
        } else {
            window.setBackgroundDrawableResource(R.drawable.app_bg)
        }
    }
}
