package io.github.arashiyama11.tinybudget


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.DpSize
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
import io.github.arashiyama11.tinybudget.data.AppContainer
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import io.github.arashiyama11.tinybudget.ui.overlay.OverlayUi
import io.github.arashiyama11.tinybudget.ui.overlay.OverlayViewModel
import io.github.arashiyama11.tinybudget.ui.overlay.OverlayViewModelFactory
import io.github.arashiyama11.tinybudget.ui.theme.TinyBudgetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
            OverlayViewModelFactory(appContainer, ::stopSelf)
        )[OverlayViewModel::class.java]
    }

    private val _onBackPressedDispatcher =
        OnBackPressedDispatcher { /* 何もしない */ }

    override val onBackPressedDispatcher: OnBackPressedDispatcher = _onBackPressedDispatcher


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
                        stopSelf()
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
        if (this::composeView.isInitialized) {
            windowManager.removeView(composeView)
        }
        _viewModelStore.clear()
        isRunning = false
        println("Acc Overlay Service destroyed")
        lifecycleScope.launch {
            settingsRepository.setOverlayDestroyedAt(System.currentTimeMillis())
        }
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)
}

@Composable
private fun DraggableOverlay(
    modifier: Modifier,
    shape: Shape,
    windowManager: WindowManager,
    host: ComposeView,
    lp: WindowManager.LayoutParams,
    settingsRepository: SettingsRepository,
    initialPosition: Pair<Float, Float>,
    initialSize: Pair<Float, Float>,
    onExit: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val screenW = remember { context.resources.displayMetrics.widthPixels.toFloat() }
    val screenH = remember { context.resources.displayMetrics.heightPixels.toFloat() }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val minWidth = 64.dp
    val minHeight = 40.dp
    // サイズ上限を画面サイズの80%に設定
    val maxWidth = remember { with(density) { (screenW * 0.8f).toDp() } }
    val maxHeight = remember { with(density) { (screenH * 0.8f).toDp() } }

    // 事前読み込みした設定値を使用してstate初期化
    val initialValues = remember {
        with(density) {
            val constrainedWidth = initialSize.first.dp.coerceIn(minWidth, maxWidth)
            val constrainedHeight = initialSize.second.dp.coerceIn(minHeight, maxHeight)

            val offsetX = initialPosition.first.coerceIn(
                0f,
                (screenW - constrainedWidth.toPx()).coerceAtLeast(0f)
            )
            val offsetY = initialPosition.second.coerceIn(
                0f,
                (screenH - constrainedHeight.toPx()).coerceAtLeast(0f)
            )

            Triple(Pair(offsetX, offsetY), constrainedWidth, constrainedHeight)
        }
    }

    var offsetX by remember { mutableFloatStateOf(initialValues.first.first) }
    var offsetY by remember { mutableFloatStateOf(initialValues.first.second) }
    var size by remember { mutableStateOf(DpSize(initialValues.second, initialValues.third)) }

    // 初期値でWindowManagerのレイアウトを即座に更新
    LaunchedEffect(Unit) {
        with(density) {
            lp.apply {
                x = offsetX.toInt()
                y = offsetY.toInt()
                width = size.width.roundToPx()
                height = size.height.roundToPx()
            }
            windowManager.updateViewLayout(host, lp)
        }
    }

    TinyBudgetTheme {
        Surface(
            modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // ドラッグ終了時に位置を保存
                            coroutineScope.launch {
                                settingsRepository.setOverlayPosition(offsetX, offsetY)
                            }
                        }
                    ) { change, drag ->
                        change.consume()
                        offsetX = (offsetX + drag.x)
                            .coerceIn(0f, (screenW - size.width.toPx()))
                        offsetY = (offsetY + drag.y)
                            .coerceIn(0f, (screenH - size.height.toPx()))
                        lp.x = offsetX.toInt()
                        lp.y = offsetY.toInt()
                        windowManager.updateViewLayout(host, lp)
                    }
                },
            shape = shape
        ) {
            Box(Modifier.fillMaxSize()) {

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    content()
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onExit() },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    Modifier
                        .size(24.dp)
                        .align(Alignment.BottomStart)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    // リサイズ終了時にサイズと位置を保存
                                    coroutineScope.launch {
                                        with(density) {
                                            settingsRepository.setOverlaySize(
                                                size.width.value,
                                                size.height.value
                                            )
                                            settingsRepository.setOverlayPosition(offsetX, offsetY)
                                        }
                                    }
                                }
                            ) { change, drag ->
                                change.consume()
                                with(density) {
                                    val dx = drag.x.toDp()
                                    val dy = drag.y.toDp()

                                    // 新しいサイズを計算（制限を適用）
                                    val newW = (size.width - dx).coerceIn(minWidth, maxWidth)
                                    val newH = (size.height + dy).coerceIn(minHeight, maxHeight)

                                    // 位置調整（左上を固定点として、サイズ変更時の位置補正）
                                    val widthDiff = size.width - newW
                                    val newOffsetX = (offsetX + widthDiff.toPx()).coerceIn(
                                        0f,
                                        (screenW - newW.toPx()).coerceAtLeast(0f)
                                    )

                                    // 状態を更新
                                    offsetX = newOffsetX
                                    size = DpSize(newW, newH)

                                    // WindowManagerのレイアウトパラメータを更新
                                    lp.apply {
                                        width = newW.roundToPx()
                                        height = newH.roundToPx()
                                        x = newOffsetX.toInt()
                                    }
                                    windowManager.updateViewLayout(host, lp)
                                }
                            }
                        }
                        .background(
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(topEnd = 4.dp)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Resize",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}