package cat.company.wandervault.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import cat.company.wandervault.domain.model.activeNotificationNextStep
import cat.company.wandervault.domain.model.computeNextStepDeadline
import cat.company.wandervault.domain.model.hasExpiredNotificationNextStep
import cat.company.wandervault.domain.repository.ActivityRepository
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import cat.company.wandervault.domain.repository.DestinationRepository
import cat.company.wandervault.domain.repository.TripDescriptionRepository
import cat.company.wandervault.domain.repository.TripRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime
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
    private val destinationRepository: DestinationRepository by inject()
    private val activityRepository: ActivityRepository by inject()
    private val tripDescriptionRepository: TripDescriptionRepository by inject()

    override suspend fun doWork(): Result {
        if (!appPreferences.getNotificationsEnabled()) return Result.success()
        if (!canPostNotifications()) return Result.success()

        val now = ZonedDateTime.now()
        val today = now.toLocalDate()
        val trips = tripRepository.getTrips().first()

        trips.forEach { trip ->
            val startDate = trip.startDate ?: return@forEach
            val endDate = trip.endDate
            val daysUntilStart = ChronoUnit.DAYS.between(today, startDate)

            val shouldNotify = daysUntilStart in 0..NOTIFY_DAYS_AHEAD ||
                (endDate != null && !today.isAfter(endDate) && !today.isBefore(startDate))

            if (shouldNotify) {
                val contentText = buildNotificationText(trip, daysUntilStart, now)
                sendNotification(trip, contentText)
            }
        }

        return Result.success()
    }

    private fun sendNotification(trip: Trip, contentText: String) {
        if (!canPostNotifications()) {
            return
        }

        val notificationTitle = trip.title.ifBlank {
            appContext.getString(R.string.notification_trip_fallback_title)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
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
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_TAG, trip.id, notification)
    }

    /**
     * Resolves the notification body for [trip].
     *
     * Reuses a stored "What's Next" notice while it is still valid, refreshes expired notices
     * from itinerary data when possible, and otherwise falls back to the existing countdown or
     * in-progress strings.
     */
    private suspend fun buildNotificationText(
        trip: Trip,
        daysUntilStart: Long,
        now: ZonedDateTime,
    ): String {
        trip.activeNotificationNextStep(now)?.let { return it }

        if (trip.hasExpiredNotificationNextStep(now)) {
            val refreshedNextStep = refreshExpiredNextStep(trip, now)
            if (!refreshedNextStep.isNullOrBlank()) return refreshedNextStep
        }

        return buildFallbackNotificationText(daysUntilStart)
    }

    /**
     * Attempts to regenerate an expired stored "What's Next" notice for [trip].
     *
     * This loads the trip itinerary and activities, asks the AI repository for a fresh notice,
     * and persists the regenerated text together with its next expiry deadline when successful.
     */
    private suspend fun refreshExpiredNextStep(trip: Trip, now: ZonedDateTime): String? {
        return try {
            val destinations = destinationRepository.getDestinationsForTrip(trip.id).first()
            val activities = activityRepository.getActivitiesForTrip(trip.id).first()
            val refreshedNextStep =
                tripDescriptionRepository.generateWhatsNext(
                    trip = trip,
                    destinations = destinations,
                    now = now,
                    activities = activities,
                )
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            if (refreshedNextStep != null) {
                tripRepository.updateTripWhatsNext(
                    tripId = trip.id,
                    nextStep = refreshedNextStep,
                    nextStepDeadline = computeNextStepDeadline(
                        destinations = destinations,
                        activities = activities,
                        now = now,
                    ),
                )
            }

            refreshedNextStep
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh expired notification text for trip ${trip.id}", e)
            null
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Builds the non-AI fallback notification body used when there is no valid stored notice and
     * an expired one could not be refreshed.
     */
    private fun buildFallbackNotificationText(daysUntilStart: Long): String {
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
        private const val TAG = "TripNotificationWorker"

        /** Notification channel ID for trip reminders. */
        const val CHANNEL_ID = "trip_reminders"

        /** Tag applied to all trip reminder notifications for targeted cancellation. */
        private const val NOTIFICATION_TAG = "trip_reminder"

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
         * Cancels the periodic notification worker and dismisses all visible trip reminder
         * notifications (identified by [NOTIFICATION_TAG]) from the notification shade.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.activeNotifications
                .filter { it.tag == NOTIFICATION_TAG }
                .forEach { manager.cancel(it.tag, it.id) }
        }
    }
}
