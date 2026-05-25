# SkyWatch - Open Source App for Realtime Flight Tracking
> Version 1.0 | Platform: Android (MVP) | Status: 1st iteration completed

---

## 1. PRODUCT OVERVIEW

**SkyWatch** is a free, real-time flight tracking Android app that notifies users when aircraft fly over their chosen location within a custom radius. It is built for aviation hobbyists, curious adults, and kids who enjoy spotting planes overhead.

### One-Line Summary
> "Your personal radar — get notified when planes fly over you."

### Core Value Proposition
- Know exactly which plane is flying over you in real-time
- Get smart, non-intrusive notifications
- Track specific flights you care about
- Keep it fun with streaks and history

---

## 2. TARGET AUDIENCE

- Aviation hobbyists and enthusiasts
- Curious adults and kids
- Plane spotters
- Anyone who looks up and wonders "what plane is that?"

**Monetization:** None. This is a free hobby app.

---

## 3. PLATFORM & TECH STACK

| Component | Choice | Reason |
|-----------|--------|--------|
| Platform | Android (MVP) | Easier to build and test for free |
| Flight Data | OpenSky Network API | Free, open-source, reliable |
| Maps | OpenStreetMap / Mapbox (free tier) | No cost, good coverage |
| Local Storage | Room Database | Free, built into Android |
| Background Tasks | WorkManager | Free, handles background tracking |
| Notifications | Local Android Notifications | No external service needed |

---

## 4. PRODUCT CONSTITUTION

### What SkyWatch IS:
- A real-time, location-based flight notification app
- A fun, lightweight hobby tool
- A free product with no ads or subscriptions
- An Android-first product (iOS and web extension are V2+)

### What SkyWatch is NOT:
- A professional aviation tool
- A replacement for FlightRadar24 or similar apps
- A monetized product
- A social or community platform (V2+ consideration)

---

## 5. CORE FEATURES (MVP - Version 1)

### 5.1 Location Tracking
- User sets ONE active tracking location (MVP)
- Options: Current GPS location OR manual address entry
- All location access requires explicit user consent

### 5.2 Custom Radius
- Slider-based radius selection
- Range: 1 km to 50 km
- User can adjust anytime in Settings

### 5.3 Time-Based Tracking
- User sets active hours: From [HH:MM] to [HH:MM] (same day)
- Option: "All day" (24/7 tracking)
- App ONLY sends notifications during the active window

### 5.4 Smart Notifications
- Real-time alerts when planes enter the chosen radius
- Shows FIRST 3 flights as individual notifications (first-come, first-served)
- Additional flights collated as: "[X] more planes passed — tap to see all"
- Tapping collated notification → opens Flight List Screen
- Notification content:
  - Flight number (e.g., AI345)
  - Time detected
  - Location info (e.g., "over Mulund")
  - "More Details" CTA button

### 5.5 Notification Limits
- User chooses max notifications per minute (e.g., 3/min, 5/min)
- Prevents notification spam
- Overflow is collated into the "[X] more planes" group

### 5.6 Track Specific Flights
- User can add up to 10 specific flight numbers to a watchlist
- Priority notifications when a tracked flight is detected nearby
- Visual distinction between tracked vs general flight notifications

### 5.7 Flight List Screen
- Shows all planes detected during the current session
- List format: Flight number, time, altitude
- Tap any flight → navigates to Flight Details Screen
- Paginated by week (current week's flights)

### 5.8 Flight Details Screen
- Minimal map showing flight trajectory
- Departure airport (name + code)
- Arrival airport (name + code)
- Current location on map
- Altitude, speed, aircraft type, heading
- Origin country

### 5.9 Background Operation
- App continues tracking when closed or minimised
- Persistent status notification: "SkyWatch is tracking..."
- WorkManager handles background API polling

### 5.10 Sound & Notification Preferences
- Sound: OFF by default
- User can enable sound in Settings
- No vibration option

### 5.11 Streak System
- Tracks consecutive days the user has active tracking sessions
- Displayed as a streak counter on HomeScreen (e.g., "🔥 5 day streak!")
- One nudge notification per day if the streak is at risk
- Resets if no tracking activity for 24 hours
- Encourages daily engagement

### 5.12 Flight History
- Stores last 7 days of detected flights locally
- Rolling window: Oldest day deleted when new day starts
- Week view: Grouped by day, expandable
  - Example: "Monday: 23 flights ▼"
- All Flights tab: Paginated scroll of full week
- Resets every Sunday midnight

---

## 6. SCREEN MAP

```
App Launch
    ↓
HomeScreen
    ├── [Settings] → SettingsScreen
    │       └── [Save] → HomeScreen
    ├── [Start/Stop Tracking] → toggles tracking ON/OFF
    └── [View All Flights] → FlightListScreen
                                └── [Tap a flight] → FlightDetailsScreen
                                                          └── [Back] → FlightListScreen

Notification (received on phone)
    ├── Tap single notification → FlightDetailsScreen
    └── Tap "[X] more planes" → FlightListScreen
```

---

## 7. APP STATE VARIABLES

| Variable Name | Type | Default | Persisted |
|--------------|------|---------|-----------|
| userLocation | String | "Current Location" | Yes |
| trackingRadius | Double | 5.0 | Yes |
| activeTimeFrom | String | "00:00" | Yes |
| activeTimeTo | String | "23:59" | Yes |
| streakCount | Integer | 0 | Yes |
| isTracking | Boolean | false | Yes |
| nearbyFlights | List<Flight> | [] | No |
| flightCallsigns | List<String> | [] | No |
| flightLatitudes | List<Double> | [] | No |
| flightLongitudes | List<Double> | [] | No |
| flightAltitudes | List<Double> | [] | No |
| flightSpeeds | List<Double> | [] | No |
| flightCountries | List<String> | [] | No |

---

## 8. FLIGHT DATA STRUCTURE

| Field | Type | Source (OpenSky Index) |
|-------|------|----------------------|
| flightId | String | states[0] |
| callsign | String | states[1] |
| originCountry | String | states[2] |
| longitude | Double | states[5] |
| latitude | Double | states[6] |
| altitude | Double | states[7] (meters) |
| onGround | Boolean | states[8] |
| speed | Double | states[9] (m/s) |
| heading | Double | states[10] |

---

## 9. OPENSKY API REFERENCE

### Endpoint
```
GET https://opensky-network.org/api/states/all
```

### Query Parameters
| Parameter | Description | Example (Mumbai) |
|-----------|-------------|-----------------|
| lamin | South latitude boundary | 19.1 |
| lomin | West longitude boundary | 72.9 |
| lamax | North latitude boundary | 19.3 |
| lomax | East longitude boundary | 73.1 |

### Notes
- Free tier: 400 API calls/day (anonymous: 100/day)
- Poll every 15-30 seconds during active hours
- CORS restriction: Works on real devices, blocked in browser-based testers
- Authentication: Basic Auth (username:password) OR anonymous

### Bounding Box Formula
To calculate the bounding box from a center point and radius:
```
1 degree latitude  ≈ 111 km
1 degree longitude ≈ 111 km × cos(latitude)

lamin = userLat - (radiusKm / 111)
lamax = userLat + (radiusKm / 111)
lomin = userLon - (radiusKm / (111 × cos(userLat)))
lomax = userLon + (radiusKm / (111 × cos(userLat)))
```

---

## 10. DOS AND DON'TS

### ✅ DO:
- Always request location permission explicitly with clear explanation
- Show a persistent "SkyWatch is tracking" notification during background tracking
- Limit notifications to user's chosen max per minute
- Collate overflow flights into "[X] more planes" group
- Store max 10 flights in the specific flight watchlist
- Keep the UI minimal and clean
- Show streak counter prominently on HomeScreen
- Send streak nudge notification once per day maximum
- Poll OpenSky every 15-30 seconds (not more frequently)
- Filter flights by radius AFTER fetching from API
- Store only 7 days of flight history locally
- Reset history every Sunday midnight

### ❌ DON'T:
- Never track location without explicit user consent
- Never send more notifications than user's chosen limit
- Never store data beyond 7 days
- Never show ALL sky traffic (only within user's radius)
- Never add vibration (removed from PRD)
- Never add monetization features in V1
- Never allow more than 10 specific tracked flights
- Never poll the API more than once per 15 seconds
- Never show a cluttered full-sky radar view

---

## 11. KEY DECISIONS LOG

| Decision | Reason |
|----------|--------|
| Android first | Easier free development and testing |
| OpenSky Network | Free, open-source, good global coverage |
| 3 notifications + collated overflow | Prevents notification spam, clean UX |
| Max 10 tracked flights | Keeps watchlist focused |
| 7-day rolling history | Balance between usefulness and storage |
| Local notifications only | No Firebase dependency in V1 |
| Silent by default | Respects user environment |
| Single location in V1 | Simplicity first, expand in V2 |
| No vibration | User requested removal |
| Streak system | Drives daily retention without monetization |

---

## 12. KNOWN TECHNICAL NOTES

- **CORS Issue:** OpenSky API is blocked in browser-based test environments (FlutterFlow test, web previews). Works correctly on real Android devices.
- **FlutterFlow Limitation:** Free tier does not support APK export, device testing, or code export. Paid plan (~$30/month) required for full functionality.
- **OpenSky Rate Limits:** Free anonymous = 100 calls/day. Registered free account = 400 calls/day. Plan polling frequency accordingly.
- **Bounding Box vs Radius:** OpenSky uses a rectangular bounding box. Radius filtering must be done in-app after receiving the data using the `isFlightInRadius` function.
- **Null Safety:** All OpenSky data fields can be null. All custom Dart functions must handle null values explicitly.

---


*Document prepared for SkyWatch v1.0 MVP*
*Target Platform: Android | Data Source: OpenSky Network | Cost: Free*
