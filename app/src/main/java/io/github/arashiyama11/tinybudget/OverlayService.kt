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
import android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.arashiyama11.tinybudget.data.local.database.AppDatabase
import io.github.arashiyama11.tinybudget.data.repository.CategoryRepository
import io.github.arashiyama11.tinybudget.data.repository.TransactionRepository
import io.github.arashiyama11.tinybudget.ui.overlay.OverlayUi
import io.github.arashiyama11.tinybudget.ui.overlay.OverlayViewModel
import io.github.arashiyama11.tinybudget.ui.overlay.OverlayViewModelFactory
import io.github.arashiyama11.tinybudget.ui.theme.TinyBudgetTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.system.measureTimeMillis

class OverlayService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner,
    OnBackPressedDispatcherOwner {
    private val appDatabase by lazy { AppDatabase.getDatabase(this) }
    private val categoryRepository by lazy { CategoryRepository(appDatabase.categoryDao()) }
    private val transactionRepository by lazy { TransactionRepository(appDatabase.transactionDao()) }

    private val _viewModelStore = ViewModelStore()

    override val viewModelStore: ViewModelStore = _viewModelStore
    private val savedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val overlayViewModel: OverlayViewModel by lazy {
        ViewModelProvider(
            this,
            OverlayViewModelFactory(categoryRepository, transactionRepository)
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
        addOverlayComposeView()
        isRunning = true
        return START_STICKY
    }

    private fun addOverlayComposeView() {
        val params = WindowManager.LayoutParams(
            WRAP_CONTENT,
            WRAP_CONTENT,
            TYPE_APPLICATION_OVERLAY,
            FLAG_NOT_TOUCH_MODAL
                    or FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            resources.displayMetrics.let { metrics ->
                x = metrics.widthPixels
                y = (metrics.heightPixels * 0.6).toInt()
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
    onExit: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(lp.x.toFloat()) }
    var offsetY by remember { mutableFloatStateOf(lp.y.toFloat()) }
    val context = LocalContext.current
    val screenW = remember { context.resources.displayMetrics.widthPixels.toFloat() }
    val screenH = remember { context.resources.displayMetrics.heightPixels.toFloat() }
    var size by remember { mutableStateOf(DpSize(128.dp, 80.dp)) }
    val minWidth = 64.dp
    val minHeight = 40.dp
    val density = LocalDensity.current

    TinyBudgetTheme {
        Surface(
            modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
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
                        tint = Color.White
                    )
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    content()
                }

                Box(
                    Modifier
                        .size(24.dp)
                        .align(Alignment.BottomStart)
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                with(density) {
                                    val dx = drag.x.toDp()
                                    val dy = drag.y.toDp()
                                    val newW = (size.width - dx).coerceAtLeast(minWidth)
                                    val newH = (size.height + dy).coerceAtLeast(minHeight)
                                    offsetX = (offsetX + drag.x).coerceIn(
                                        0f,
                                        (screenW - newW.toPx()).coerceAtLeast(0f)
                                    )
                                    size = DpSize(newW, newH)
                                    lp.apply {
                                        width = newW.roundToPx()
                                        height = newH.roundToPx()
                                        x = offsetX.toInt()
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

@Composable
fun CurrentTimeText() {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    var currentTime by remember { mutableStateOf(LocalTime.now().format(formatter)) }
    LaunchedEffect(Unit) {
        while (true) {
            measureTimeMillis {
                currentTime = LocalTime.now().format(formatter)
            }
            delay(1_00L)
        }
    }
    Text(text = currentTime)
}


