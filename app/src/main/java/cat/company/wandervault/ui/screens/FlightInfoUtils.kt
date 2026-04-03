package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.TransportLeg
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Returns a copy of this [TransportLeg] with fields filled in from [flightInfo].
 *
 * Text fields (company, flightNumber, reservationConfirmationNumber, stopName) follow the
 * **fill-blank** rule: an existing non-blank value is preserved; only missing/blank values are
 * replaced with the extracted value.
 *
 * Datetime fields are updated as follows:
 * - **departureDateTime**: if [flightInfo] carries a `departureTime`, the time portion of the
 *   leg's existing `departureDateTime` is replaced while the date and zone are preserved.
 *   When the leg has no `departureDateTime` but both [FlightInfo.departureDate] and
 *   [FlightInfo.departureTime] are available, a new [ZonedDateTime] is constructed using the
 *   system default zone.
 * - **arrivalDateTime**: if [flightInfo] carries an `arrivalTime` and the leg already has an
 *   `arrivalDateTime`, only the time portion is updated while the date and zone are preserved.
 *   No new `arrivalDateTime` is created from scratch because the document rarely carries a
 *   reliable arrival date independently of the departure date.
 */
internal fun TransportLeg.applyFlightInfo(flightInfo: FlightInfo): TransportLeg {
    val newDepartureDateTime: ZonedDateTime? = when {
        flightInfo.departureTime != null && departureDateTime != null ->
            departureDateTime.with(flightInfo.departureTime)
        flightInfo.departureTime != null && flightInfo.departureDate != null ->
            ZonedDateTime.of(flightInfo.departureDate, flightInfo.departureTime, ZoneId.systemDefault())
        else -> departureDateTime
    }
    val newArrivalDateTime: ZonedDateTime? = when {
        flightInfo.arrivalTime != null && arrivalDateTime != null ->
            arrivalDateTime.with(flightInfo.arrivalTime)
        else -> arrivalDateTime
    }
    return copy(
        company = company?.ifBlank { null } ?: flightInfo.airline,
        flightNumber = flightNumber?.ifBlank { null } ?: flightInfo.flightNumber,
        reservationConfirmationNumber = reservationConfirmationNumber
            ?.ifBlank { null } ?: flightInfo.bookingReference,
        stopName = stopName?.ifBlank { null } ?: flightInfo.arrivalPlace,
        departureDateTime = newDepartureDateTime,
        arrivalDateTime = newArrivalDateTime,
    )
}
