package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.GeocoderRepository

/**
 * Use-case that converts a location name into GPS coordinates.
 *
 * Returns a [Pair] of (latitude, longitude) in decimal degrees (WGS-84),
 * or `null` when the name could not be geocoded.
 */
class GeocodeLocationUseCase(private val repository: GeocoderRepository) {
    suspend operator fun invoke(locationName: String): Pair<Double, Double>? =
        repository.geocode(locationName)
}
