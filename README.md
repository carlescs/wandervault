# WanderVault

An Android travel planning app powered by ML Kit and Gemini Nano that helps you organise trips, manage itineraries, and extract information from travel documents using on-device AI.

---

## Features

- **Trip Management** – Create, edit, and delete trips with cover images, date ranges, and AI-generated descriptions.
- **Itinerary Planning** – Add destinations to a trip, set arrival and departure times, and view them in chronological order.
- **Transport Tracking** – Record transport legs between destinations (flight, train, bus, ferry, driving, cycling, walking) with carrier, flight number, and booking reference details.
- **Accommodation** – Attach hotel information (name, address, reservation number) to each destination.
- **Activities** – Log per-destination activities (title, description, date/time, confirmation number) to keep track of excursions and bookings.
- **Document Organisation** – Upload and organise travel documents (PDFs, images, plain text) into a folder hierarchy per trip.
- **AI Document Extraction** – On-device OCR + Gemini Nano automatically extracts flight details (airline, flight number, booking reference, route) and hotel details (name, check-in/out dates, booking reference) from uploaded documents, and matches them to your itinerary.
- **AI Document Q&A** – Ask free-form questions about any uploaded document; Gemini Nano answers on-device without an internet connection.
- **AI Auto-Organize** – Gemini Nano suggests a folder structure for a trip's documents and moves them automatically.
- **AI Trip Descriptions** – Gemini Nano generates short, engaging trip summaries based on your itinerary, with no internet connection required.
- **AI What's Next** – Gemini Nano produces a concise "what's next" notice for the current moment in an active trip.
- **Calendar View** – Browse all trip events on a monthly calendar.
- **Share-to-App** – Share a document directly from any app to WanderVault; it is saved to your chosen trip and analysed automatically.
- **Favourites** – Mark trips as favourites for quick access.
- **Archive** – Archive completed trips to keep the main list tidy while retaining all data.
- **Notifications** – Daily trip reminders delivered via WorkManager.
- **Settings** – Toggle trip-reminder notifications and manage app preferences.
- **Backup & Restore** – Export and import the full database for safe storage.
- **Adaptive Layout** – Material 3 UI scales to phones and tablets.

---

## Architecture

WanderVault follows **Clean Architecture** with three distinct layers:

```
cat.company.wandervault/
├── data/
│   ├── di/                  # DataModule (Koin)
│   ├── local/               # Room entities, DAOs, database, type converters
│   ├── mlkit/               # ML Kit implementations (Gemini Nano, OCR)
│   └── repository/          # Repository implementations
├── domain/
│   ├── di/                  # DomainModule (Koin)
│   ├── model/               # Pure Kotlin domain models
│   ├── repository/          # Repository interfaces
│   └── usecase/             # 50+ single-responsibility use cases
└── ui/
    ├── di/                  # PresentationModule (Koin, ViewModels)
    ├── navigation/          # NavHost and route definitions
    ├── screens/             # Composable screens, ViewModels, UiState classes
    └── theme/               # Material 3 colours, typography, shapes
```

| Layer        | May depend on       | Must NOT depend on         |
|--------------|---------------------|----------------------------|
| Presentation | Domain              | Data, Room, Koin internals |
| Domain       | –                   | Presentation, Data, Android|
| Data         | Domain (interfaces) | Presentation               |

---

## Tech Stack

| Category          | Library / Tool                             | Version        |
|-------------------|--------------------------------------------|----------------|
| Language          | Kotlin                                     | 2.3.10         |
| UI                | Jetpack Compose (BOM)                      | 2026.02.01     |
| Design system     | Material 3 + Adaptive Navigation Suite     | –              |
| Navigation        | Jetpack Navigation Compose                 | 2.8.9          |
| ViewModel         | Jetpack Lifecycle                          | 2.10.0         |
| Database          | Room                                       | 2.8.4          |
| Dependency injection | Koin                                    | 4.1.1          |
| Image loading     | Coil                                       | 2.7.0          |
| On-device AI      | ML Kit GenAI (Gemini Nano Prompt API)      | 1.0.0-beta1    |
| OCR               | ML Kit Text Recognition                    | 16.0.1         |
| Background work   | WorkManager                                | 2.10.1         |
| Coroutines        | kotlinx-coroutines                         | 1.10.2         |
| Symbol processing | KSP                                        | 2.3.2          |
| Min SDK           | 26 (Android 8.0)                           | –              |
| Target SDK        | 36 (Android 16 preview)                    | –              |

---

## Getting Started

### Prerequisites

- Android Studio Ladybug or later
- JDK 21
- An Android device or emulator running **Android 8.0 (API 26)** or higher
- A device that supports the **Gemini Nano** on-device model is required for AI features (optional for core functionality)

### Build

```bash
# Clone the repository
git clone https://github.com/carlescs/wandervault.git
cd wandervault

# Build a debug APK
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```

### Run Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires a connected device or emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Kotlin code style
./gradlew ktlintCheck
```

---

## Key Domain Models

| Model                   | Description                                                      |
|-------------------------|------------------------------------------------------------------|
| `Trip`                  | Root entity: title, cover image, dates, AI description, favourites flag |
| `Destination`           | A stop within a trip: name, position, arrival/departure times, notes |
| `Transport`             | Transport from a destination, composed of one or more `TransportLeg`s |
| `TransportLeg`          | Individual leg: type (flight, train…), carrier, flight number, booking ref |
| `Hotel`                 | Accommodation attached to a destination                          |
| `Activity`              | A planned activity at a destination: title, description, date/time, confirmation number |
| `TripDocument`          | An uploaded file (PDF, image, text) with an optional AI summary  |
| `TripDocumentFolder`    | Hierarchical folder for organising documents                     |
| `DocumentExtractionResult` | AI extraction output: summary, `FlightInfo`, `HotelInfo`     |
| `FlightInfo`            | Extracted flight data: airline, flight number, route, booking ref |
| `HotelInfo`             | Extracted hotel data: name, address, booking ref, check-in/out dates |
| `OrganizationPlan`      | AI-suggested folder structure for a trip's documents             |

---

## ML Kit Integration

### Gemini Nano – Trip Descriptions (`TripDescriptionRepositoryImpl`)

Uses the **ML Kit GenAI Prompt API** to generate 2–3 sentence trip summaries on-device. The repository checks model availability, triggers a download if needed, and streams the response asynchronously.

### Gemini Nano – "What's Next" (`TripDescriptionRepositoryImpl`)

Generates a concise notice describing the traveller's current or imminent itinerary item. The prompt includes all destination arrival/departure times so the model can take time zones into account. The result is displayed on the trip detail screen and stored for offline access. Trip Descriptions and "What's Next" share `TripDescriptionRepositoryImpl` because both features use the same Gemini Nano session and on-device model lifecycle.

### Gemini Nano – Document Q&A (`DocumentSummaryRepositoryImpl`)

After a document is processed, users can ask free-form questions about it. The extracted document text and the user's question are sent to Gemini Nano, which answers on-device without an internet connection. Document Q&A and Auto-Organize both use `DocumentSummaryRepositoryImpl` because they share the same document-text pipeline and Gemini Nano session management.

### Gemini Nano – Auto-Organize (`DocumentSummaryRepositoryImpl`)

Analyses all documents in a trip and suggests a folder hierarchy. The model returns a `folderName → [document]` mapping (`OrganizationPlan`) that the app applies automatically when the user confirms.

### Gemini Nano + OCR – Document Intelligence (`DocumentSummaryRepositoryImpl`)

Supports **PDF**, **image**, and **plain-text** files:

1. **Text extraction**: plain text is read directly; PDFs are rendered with `PdfRenderer` + ML Kit Text Recognition; images use Text Recognition OCR.
2. **AI analysis**: the extracted text is sent to Gemini Nano with a structured prompt that identifies whether the document is a flight booking, hotel reservation, or other travel document.
3. **Structured output**: the result is parsed into `FlightInfo` or `HotelInfo`, which are then matched against the trip's existing destinations to pre-populate the itinerary automatically.

---

## Database

Room database with **23 schema versions** (`WanderVaultDatabase`). All migrations are explicit; `fallbackToDestructiveMigration()` is only allowed in debug builds.

Entities: `TripEntity`, `DestinationEntity`, `TransportEntity`, `TransportLegEntity`, `HotelEntity`, `ActivityEntity`, `TripDocumentEntity`, `TripDocumentFolderEntity`.

---

## License

```
Copyright 2024 WanderVault contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
