package io.github.arashiyama11.tinybudget


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.arashiyama11.tinybudget.di.AppContainer
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import io.github.arashiyama11.tinybudget.ui.overlay.DraggableOverlay
import io.github.arashiyama11.tinybudget.ui.overlay.OverlayUi
import io.github.arashiyama11.tinybudget.ui.overlay.OverlayViewModel
import io.github.arashiyama11.tinybudget.ui.overlay.OverlayViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class OverlayService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner,
    OnBackPressedDispatcherOwner {
    private lateinit var appContainer: AppContainer
    private lateinit var settingsRepository: SettingsRepository

    private val _viewModelStore = ViewModelStore()

    override val viewModelStore: ViewModelStore = _viewModelStore
    private val savedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val overlayViewModel: OverlayViewModel by lazy {
        ViewModelProvider(
            this,
            OverlayViewModelFactory(appContainer, ::shutdown)
        )[OverlayViewModel::class.java]
    }

    private val _onBackPressedDispatcher =
        OnBackPressedDispatcher { /* 何もしない */ }

    override val onBackPressedDispatcher: OnBackPressedDispatcher = _onBackPressedDispatcher

    private var hasShutdown = false


    companion object {
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIF_ID = 1001
        private var isRunning = false
    }

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView

    override fun onCreate() {
        super.onCreate()
        appContainer = (application as TinyBudgetApp).appContainer
        settingsRepository = appContainer.settingsRepository
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)


        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (isRunning) {
            Log.d("OverlayService", "Service is already running or started with invalid ID")
            return START_STICKY
        }

        // 設定を事前に読み込んでからUIを構築
        lifecycleScope.launch {
            val savedPosition = settingsRepository.overlayPosition.first()
            val savedSize = settingsRepository.overlaySize.first()
            addOverlayComposeView(savedPosition, savedSize)
        }

        isRunning = true
        return START_STICKY
    }

    private fun addOverlayComposeView(
        savedPosition: Pair<Float, Float>,
        savedSize: Pair<Float, Float>
    ) {
        val params = WindowManager.LayoutParams(
            WRAP_CONTENT,
            WRAP_CONTENT,
            TYPE_APPLICATION_OVERLAY,
            FLAG_NOT_TOUCH_MODAL
                    or FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // 初期位置は後でsettingsから読み込んだ値で更新される
            resources.displayMetrics.let { metrics ->
                x = savedPosition.first.toInt()
                y = savedPosition.second.toInt()
            }
            softInputMode = WindowManager.LayoutParams
                .SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        composeView = ComposeView(this).apply {

            isFocusable = true
            isFocusableInTouchMode = true


            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewTreeOnBackPressedDispatcherOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)

            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            setContent {
                DraggableOverlay(
                    modifier = Modifier,
                    shape = RoundedCornerShape(16.dp),
                    windowManager = windowManager,
                    host = this,
                    lp = params,
                    settingsRepository = settingsRepository,
                    initialPosition = savedPosition,
                    initialSize = savedSize,
                    onExit = {
                        runBlocking {
                            shutdown()
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        OverlayUi(overlayViewModel)
                    }
                }
            }
        }
        windowManager.addView(composeView, params)
    }


    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Service 実行中")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val chan = NotificationChannel(
            CHANNEL_ID,
            "Overlay Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "画面上にボタンを表示し、キャプチャを行う"
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(chan)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!hasShutdown) {
            shutdown()
        }
    }

    // 一旦はrunBlockingが安定かも
    fun shutdown() = runBlocking {
        if (hasShutdown) return@runBlocking
        hasShutdown = true
        runCatching {
            if (this@OverlayService::composeView.isInitialized && composeView.isAttachedToWindow) {
                windowManager.removeViewImmediate(composeView)
            }
            _viewModelStore.clear()
            isRunning = false

            settingsRepository.setOverlayDestroyedAt(System.currentTimeMillis())

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }.onFailure {
            it.printStackTrace()
            Log.e("OverlayService", "Error during shutdown", it)
        }
    }
}
