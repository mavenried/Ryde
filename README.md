# RYDE

A minimal Android fitness tracking app for cycling, running, and walking. Tracks routes with GPS, shows a live map, and keeps a history of past rides.

## Features

- **Live tracking** — real-time GPS speed/pace, distance, elapsed time, and calorie estimate
- **Three activity types** — cycling (speed), running & walking (pace)
- **Google Maps** — live route drawn in colour-coded speed segments; dark mode uses an invert + hue-rotate filter so the map looks natural
- **Auto-pause** — pauses automatically when you stop moving, resumes when you start again
- **Recenter button** — snaps the camera back to your position while riding
- **History** — scrollable list of past routes with a stats summary and speed-coloured route replay
- **Elevation profile** — chart of altitude over distance with an 8 m noise gate to avoid false gain from GPS jitter
- **GPX export** — share any saved route as a standard `.gpx` file
- **Music controls** — playback and volume controls sourced from the notification listener, with a live progress bar
- **Short-ride guard** — asks for confirmation before saving a ride under 1 minute or 0.1 km

## Requirements

- Android 8.0+ (API 26)
- A Google Maps API key (see setup below)
- Location permission (fine + background for continuous tracking)
- Notification listener permission (for music controls)

## Setup

1. Clone the repo.
2. Add your Maps API key to `app/src/main/AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_KEY_HERE" />
   ```
3. Open in Android Studio, sync Gradle, and run.

## Tech stack

| Layer      | Library                             |
| ---------- | ----------------------------------- |
| UI         | Jetpack Compose + Material 3        |
| Maps       | Google Maps SDK 18 + maps-compose 6 |
| Navigation | Navigation Compose                  |
| Database   | Room 2.6                            |
| Location   | FusedLocationProviderClient         |
| Media      | NotificationListenerService         |
| SVG        | AndroidSVG 1.4                      |
| Language   | Kotlin 2.0 / coroutines             |

## Project structure

```
app/src/main/kotlin/me/mavenried/Ryde/
├── data/           Room entities, DAOs, AppDatabase
├── domain/         Clean models, RouteRepository, TrackStats
├── service/        TrackingService (foreground), MediaListenerService
├── ui/
│   ├── components/ Reusable composables (map views, panels, charts)
│   ├── screens/    HomeScreen, HistoryScreen, RouteDetailScreen, SettingsScreen
│   ├── viewmodel/  TrackingViewModel, HistoryViewModel, RouteDetailViewModel
│   └── theme/      Material 3 colour scheme and typography
└── util/           GpxExporter, ReverseGeocoder, PermissionHelper, UserPrefs
```
