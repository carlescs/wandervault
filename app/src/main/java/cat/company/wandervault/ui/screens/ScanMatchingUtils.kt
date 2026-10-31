package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.ScannedDocumentInfo
import cat.company.wandervault.domain.model.TransportType

/**
 * Suggests which of [legs] the fields of a scanned document most likely belong to.
 *
 * In order of preference:
 * 1. A leg whose flight number matches the scanned flight number.
 * 2. A leg whose confirmation number matches the scanned booking reference.
 * 3. When a flight number was scanned, the first flight leg without a flight number.
 * 4. The first leg with neither a flight number nor a confirmation number.
 * 5. The only leg, when there is exactly one.
 *
 * @return The index of the suggested leg, or `null` to suggest creating a new leg.
 */
internal fun suggestLegIndexForScan(
    legs: List<TransportLegEditState>,
    info: ScannedDocumentInfo,
): Int? {
    if (legs.isEmpty()) return null
    val flight = info.flightNumber?.normalizedCode()
    val booking = info.bookingReference?.normalizedCode()

    if (flight != null) {
        legs.indexOfFirst { it.flightNumber.normalizedCode() == flight }.takeIf { it >= 0 }?.let { return it }
    }
    if (booking != null) {
        legs.indexOfFirst { it.confirmationNumber.normalizedCode() == booking }.takeIf { it >= 0 }?.let { return it }
    }
    if (flight != null) {
        legs.indexOfFirst { it.typeName == TransportType.FLIGHT.name && it.flightNumber.isBlank() }
            .takeIf { it >= 0 }?.let { return it }
    }
    legs.indexOfFirst { it.flightNumber.isBlank() && it.confirmationNumber.isBlank() }
        .takeIf { it >= 0 }?.let { return it }
    return if (legs.size == 1) 0 else null
}

/** Upper-cases and strips whitespace so "vy 1234" and "VY1234" compare equal. */
private fun String.normalizedCode(): String = filterNot { it.isWhitespace() }.uppercase()
