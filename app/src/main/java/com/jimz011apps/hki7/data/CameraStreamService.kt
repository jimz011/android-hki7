package com.jimz011apps.hki7.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.util.Size
import android.view.Display
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.jimz011apps.hki7.R
import java.net.InetAddress
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Opt-in MJPEG camera stream for wall tablets. Home Assistant cannot create a camera entity through
 * mobile_app, so this service binds the tablet camera and serves JPEG frames on the LAN instead.
 *
 * Camera is a while-in-use foreground-service type: Android 12+ rejects starting it from
 * [android.content.Intent.ACTION_BOOT_COMPLETED]. The stream is started when HKI 7 is in the
 * foreground (see [sync]).
 */
class CameraStreamService : Service(), LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var prefs: PreferencesManager
    private var observeJob: Job? = null
    private var server: MjpegHttpServer? = null
    private var boundPort: Int? = null
    private var boundFacing: String? = null
    private var boundToken: String? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
        createChannel()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasCameraPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!startCameraForeground()) {
            return START_NOT_STICKY
        }
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        if (observeJob?.isActive != true) {
            observeJob = scope.launch {
                prefs.devicePanelSettings
                    .map { Triple(it.cameraStreamEnabled, it.clampedStreamPort(), it) }
                    .distinctUntilChanged()
                    .collect { (enabled, port, settings) ->
                        if (!enabled || !hasCameraPermission()) {
                            stopSelf()
                            return@collect
                        }
                        val ensured = settings.withStreamToken()
                        if (ensured.streamToken != settings.streamToken) {
                            prefs.saveDevicePanelSettings(ensured)
                            return@collect
                        }
                        bindCameraAndServer(port, ensured.cameraFacing, ensured.streamToken)
                    }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        observeJob?.cancel()
        scope.cancel()
        unbindCamera()
        server?.stop()
        server = null
        cameraExecutor.shutdown()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    private suspend fun bindCameraAndServer(port: Int, facing: String, token: String) {
        if (boundPort == port && boundFacing == facing && boundToken == token && server != null) return
        unbindCamera()
        server?.stop()
        server = null
        _lastError.value = null
        val bindAddress = withContext(Dispatchers.IO) { lanBindAddress() }
        if (bindAddress == null) {
            _lastError.value = "no_lan"
            return
        }
        val next = MjpegHttpServer(port, token)
        val started = withContext(Dispatchers.IO) { next.start(bindAddress) }
        started.onFailure { error ->
            _lastError.value = error.message ?: error.javaClass.simpleName
            next.stop()
            return
        }
        server = next
        boundPort = port
        boundFacing = facing
        boundToken = token
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            runCatching { attachCamera(providerFuture.get(), facing) }.onFailure { error ->
                _lastError.value = error.message ?: error.javaClass.simpleName
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun lanBindAddress(): InetAddress? {
        val ip = localIpv4(this)
        if (ip.isBlank()) return null
        return runCatching { InetAddress.getByName(ip) }.getOrNull()
    }

    private fun attachCamera(provider: ProcessCameraProvider, facing: String) {
        cameraProvider = provider
        val selector = if (facing == DEVICE_CAMERA_BACK) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setTargetRotation(streamDisplayRotation(getSystemService(DisplayManager::class.java)))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(cameraExecutor) { image ->
            runCatching { image.toJpegBytes()?.let { MjpegFrameHub.publish(it) } }
            image.close()
        }
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(this, selector, analysis)
        }.onFailure {
            runCatching {
                provider.unbindAll()
                val fallback = if (facing == DEVICE_CAMERA_BACK) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                provider.bindToLifecycle(this, fallback, analysis)
            }
        }
    }

    private fun unbindCamera() {
        runCatching { cameraProvider?.unbindAll() }
        cameraProvider = null
        boundPort = null
        boundFacing = null
        boundToken = null
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    /** @return false when Android rejected a background camera FGS start. */
    private fun startCameraForeground(): Boolean {
        val notification = buildNotification()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (error: Exception) {
            _lastError.value = error.message ?: error.javaClass.simpleName
            stopSelf()
            false
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.camera_stream_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.camera_stream_channel_description)
            }
        )
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.camera_stream_notification_title))
            .setContentText(getString(R.string.camera_stream_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "hki7_camera_stream"
        private const val NOTIFICATION_ID = 4712

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError.asStateFlow()

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, CameraStreamService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, CameraStreamService::class.java)) }
            _lastError.value = null
        }

        fun sync(context: Context, settings: DevicePanelSettings) {
            if (settings.cameraStreamEnabled) start(context) else stop(context)
        }

        suspend fun syncFromPrefs(context: Context) {
            val prefs = PreferencesManager(context.applicationContext)
            prefs.ensureHomeAssistantInstanceStore()
            sync(context, prefs.devicePanelSettings.first())
        }
    }
}

/** Rotation for CameraX from a Service. `Context.display` throws on a non-visual context. */
internal fun streamDisplayRotation(manager: DisplayManager?): Int =
    runCatching { manager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation }.getOrNull()
        ?: Surface.ROTATION_0

