package cat.company.wandervault.data.repository

import android.content.Context
import android.location.Geocoder
import android.os.Build
import cat.company.wandervault.domain.repository.GeocoderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val GEOCODE_TIMEOUT_MS = 10_000L

class GeocoderRepositoryImpl(private val context: Context) : GeocoderRepository {

    override suspend fun geocode(locationName: String): Pair<Double, Double>? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: use the non-blocking listener-based API, guarded with a timeout
            // in case the callback is delayed or the service is unavailable.
            withTimeoutOrNull(GEOCODE_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(locationName, 1) { addresses ->
                        if (continuation.isActive) {
                            val address = addresses.firstOrNull()
                            continuation.resume(
                                if (address != null) Pair(address.latitude, address.longitude) else null,
                            )
                        }
                    }
                }
            }
        } else {
            // API < 33: blocking call — must run on IO dispatcher.
            withTimeoutOrNull(GEOCODE_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(locationName, 1)
                    val address = addresses?.firstOrNull() ?: return@withContext null
                    Pair(address.latitude, address.longitude)
                }
            }
        }
    }
}
