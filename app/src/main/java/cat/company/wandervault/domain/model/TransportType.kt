package cat.company.wandervault.domain.model

/**
 * The mode of transport used to travel from one itinerary destination to the next.
 *
 * Stored as the name string in the database and as [TransportType] in the domain/presentation
 * layers. Unknown or missing values are mapped to `null` when reading from the database.
 */
enum class TransportType {
    WALKING,
    CYCLING,
    DRIVING,
    BUS,
    TRAIN,
    FERRY,
    FLIGHT,
    OTHER,
}
