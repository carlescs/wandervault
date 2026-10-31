package cat.company.wandervault.data.mlkit

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspends until this Google Play Services [Task] completes and returns its result.
 *
 * Throws the task's exception on failure and a [CancellationException] when the task is cancelled.
 */
internal suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { e -> cont.resumeWithException(e) }
    addOnCanceledListener { cont.cancel(CancellationException("ML Kit task was cancelled")) }
}
