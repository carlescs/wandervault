package cat.company.wandervault.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cat.company.wandervault.MainActivity
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import cat.company.wandervault.domain.repository.TripRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * A [CoroutineWorker] that checks for upcoming trips once per day and fires a notification for
 * each trip that starts within the next [NOTIFY_DAYS_AHEAD] days or is currently ongoing.
 *
 * The notification content is the trip's "What's Next" AI notice when available, falling back to a
 * countdown message. Tapping the notification opens the trip detail screen.
 *
 * Scheduling is managed via [schedule] and [cancel] companion helpers; the unique work name
 * is [WORK_NAME].
 */
class TripNotificationWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val tripRepository: TripRepository by inject()
    private val appPreferences: AppPreferencesRepository by inject()

    override suspend fun doWork(): Result {
        if (!appPreferences.getNotificationsEnabled()) return Result.success()

        val today = LocalDate.now()
        val trips = tripRepository.getTrips().first()

        trips.forEach { trip ->
            val startDate = trip.startDate ?: return@forEach
            val endDate = trip.endDate
            val daysUntilStart = ChronoUnit.DAYS.between(today, startDate)

            val shouldNotify = daysUntilStart in 0..NOTIFY_DAYS_AHEAD ||
                (endDate != null && !today.isAfter(endDate) && today.isAfter(startDate))

            if (shouldNotify) {
                sendNotification(trip, daysUntilStart)
            }
        }

        return Result.success()
    }

    private fun sendNotification(trip: Trip, daysUntilStart: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentText = buildNotificationText(trip, daysUntilStart)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_TRIP_ID, trip.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            trip.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(trip.title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(appContext).notify(trip.id, notification)
    }

    private fun buildNotificationText(trip: Trip, daysUntilStart: Long): String {
        val nextStep = trip.nextStep
        if (!nextStep.isNullOrBlank()) return nextStep

        return when {
            daysUntilStart == 0L -> appContext.getString(R.string.notification_trip_starts_today)
            daysUntilStart == 1L -> appContext.getString(R.string.notification_trip_starts_tomorrow)
            daysUntilStart > 1L -> appContext.resources.getQuantityString(
                R.plurals.notification_trip_starts_in_days,
                daysUntilStart.toInt(),
                daysUntilStart.toInt(),
            )
            else -> appContext.getString(R.string.notification_trip_in_progress)
        }
    }

    companion object {
        /** Notification channel ID for trip reminders. */
        const val CHANNEL_ID = "trip_reminders"

        /** Unique name used with [WorkManager.enqueueUniquePeriodicWork]. */
        const val WORK_NAME = "trip_notification_check"

        /** Number of days ahead of the trip start to start sending notifications. */
        private const val NOTIFY_DAYS_AHEAD = 7L

        /**
         * Creates the [CHANNEL_ID] notification channel.  Safe to call multiple times; Android
         * ignores duplicate channel creation after the first.
         */
        fun createNotificationChannel(context: Context) {
            val name = context.getString(R.string.notification_channel_name)
            val description = context.getString(R.string.notification_channel_description)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                this.description = description
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        /**
         * Enqueues a unique periodic [TripNotificationWorker] that runs once a day.
         * Uses [ExistingPeriodicWorkPolicy.KEEP] so repeated calls are no-ops when
         * the worker is already enqueued.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TripNotificationWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /**
         * Cancels the periodic notification worker and dismisses all trip notifications that are
         * currently visible in the notification shade.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            NotificationManagerCompat.from(context).cancelAll()
        }
    }
}
