package net.luisico.howmanyhours.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import net.luisico.howmanyhours.MainActivity
import kotlinx.coroutines.*

class TimeTrackingNotificationService : Service() {
    
    companion object {
        private const val TAG = "TimeTrackingNotificationService"
        const val CHANNEL_ID = "time_tracking_clean"
        const val NOTIFICATION_ID = 1
        const val EXTRA_PROJECT_NAME = "project_name"
        const val EXTRA_START_TIME = "start_time"
        const val ACTION_START_TRACKING = "start_tracking"
        const val ACTION_STOP_TRACKING = "stop_tracking"
        
        fun startService(context: Context, projectName: String, startTime: Long) {
            Log.d(TAG, "Starting notification service for project: $projectName")
            
            // Ensure notification channel exists before starting service
            createNotificationChannelStatic(context)
            
            // Give the channel creation a moment to complete
            Thread.sleep(50)
            
            val intent = Intent(context, TimeTrackingNotificationService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_PROJECT_NAME, projectName)
                putExtra(EXTRA_START_TIME, startTime)
            }
            try {
                context.startForegroundService(intent)
                Log.d(TAG, "Foreground service started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
            }
        }
        
        private fun createNotificationChannelStatic(context: Context) {
            Log.d(TAG, "Creating notification channel statically")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Time Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows current time tracking status"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created statically")
        }
        
        fun stopService(context: Context) {
            Log.d(TAG, "Stopping notification service")
            val intent = Intent(context, TimeTrackingNotificationService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null
    private var projectName: String = ""
    private var startTime: Long = 0
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate called")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        
        // Always start foreground immediately to comply with Android requirements
        if (intent?.action == ACTION_START_TRACKING) {
            projectName = intent.getStringExtra(EXTRA_PROJECT_NAME) ?: "Unknown Project"
            startTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
            Log.d(TAG, "Starting tracking for project: $projectName, startTime: $startTime")
            
            // Ensure channel is created in service too
            createNotificationChannel()
            
            try {
                // Small delay to ensure channel is ready
                Thread.sleep(100)
                val notification = createNotification(0)
                Log.d(TAG, "Created notification successfully")
                startForeground(NOTIFICATION_ID, notification)
                Log.d(TAG, "Started foreground with notification")
                startUpdatingNotification()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground", e)
                e.printStackTrace()
            }
        } else if (intent?.action == ACTION_STOP_TRACKING) {
            Log.d(TAG, "Stopping tracking")
            stopSelf()
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        Log.d(TAG, "Creating notification channel")
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Time Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows current time tracking status"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created")
    }
    
    private fun createNotification(elapsedMinutes: Long): Notification {
        Log.d(TAG, "Creating notification for $elapsedMinutes minutes")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        
        val contentText = if (elapsedMinutes == 0L) {
            "Just started"
        } else {
            "${elapsedMinutes}m"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking: $projectName")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    private fun startUpdatingNotification() {
        Log.d(TAG, "Starting notification updates")
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                val elapsedTime = System.currentTimeMillis() - startTime
                val elapsedMinutes = elapsedTime / (60 * 1000)
                
                Log.d(TAG, "Updating notification: ${elapsedMinutes}m elapsed")
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createNotification(elapsedMinutes))
                
                delay(60000) // Update every minute
            }
        }
    }
}