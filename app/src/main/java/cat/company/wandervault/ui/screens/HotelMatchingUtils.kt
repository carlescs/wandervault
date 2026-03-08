package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.HotelInfo

/**
 * Returns `true` when the destination's stay period overlaps with the hotel check-in /
 * check-out window extracted from the document.
 *
 * Overlap is defined as: the destination's arrival date is not after the hotel check-out
 * date AND the destination's departure date is not before the hotel check-in date.
 * Missing dates on either side are treated as an open bound (no constraint).
 */
internal fun Destination.overlapsHotelDates(hotelInfo: HotelInfo): Boolean {
    val destArrival = arrivalDateTime?.toLocalDate()
    val destDeparture = departureDateTime?.toLocalDate()
    val checkIn = hotelInfo.checkInDate
    val checkOut = hotelInfo.checkOutDate
    if (checkIn != null && destDeparture != null && destDeparture.isBefore(checkIn)) return false
    if (checkOut != null && destArrival != null && destArrival.isAfter(checkOut)) return false
    return true
}
