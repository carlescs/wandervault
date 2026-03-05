package cat.company.wandervault.domain.repository

/**
 * Repository for converting a human-readable location name into GPS coordinates.
 *
 * Implementations should use the best available on-device or network geocoding provider.
 */
interface GeocoderRepository {
    /**
     * Looks up GPS coordinates for [locationName].
     *
     * @return A [Pair] of (latitude, longitude) in decimal degrees (WGS-84),
     *   or `null` when no result is found or geocoding is unavailable.
     */
    suspend fun geocode(locationName: String): Pair<Double, Double>?
}
