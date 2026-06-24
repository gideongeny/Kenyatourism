# Tembea Kenya — Release Document

**Release date:** June 24, 2026  
**Version:** 2.4.4 (versionCode 18)  
**Package name:** `com.gideongeng.kenyatourism`  
**Author:** Gideon Ngeno

---

## Build Artifacts

| Artifact | Path | Size |
|----------|------|------|
| **Release APK** | `app/build/outputs/apk/release/app-release.apk` | ~143 MB |
| **Release AAB** | `app/build/outputs/bundle/release/app-release.aab` | ~144 MB |

> **Note:** Upload the **AAB** to Google Play Console. Use the **APK** for direct distribution, sideloading, or testing on devices.

### Build Command

```bash
./gradlew assembleRelease bundleRelease
```

### Signing

- Signed with `app/release.keystore` (alias: `release_key`)
- Upload certificate: `upload_certificate.pem` (for Play App Signing enrollment)

---

## App Summary

**Tembea Kenya** (*Tembea* = “travel” in Swahili) is a premium Kenya tourism companion app. It helps travelers discover 120+ destinations — from the Maasai Mara and Diani Beach to Mount Kenya and Lamu — with an AI safari guide, interactive maps, wishlists, community reviews, and multi-language support.

**Tagline:** *Discover. Explore. Experience. Magically.*

---

## Play Store Listing

### Short Description (80 characters max)

```
Discover Kenya's 120+ destinations with AI guide, maps, reviews & wishlists.
```
*(79 characters)*

### Full Description

```
Karibu Kenya! Tembea Kenya is your world-class safari companion for exploring Africa's most beautiful country.

DISCOVER 120+ DESTINATIONS
Browse beaches, national parks, mountains, cultural sites, and hidden gems across Kenya. Search by name, filter by category, and dive into rich destination details with photos, videos, weather, and travel tips.

SAFARI AI GUIDE
Ask anything about Kenya — best time to visit, what to pack, safari tips, local culture, and more. Your personal AI travel assistant is ready 24/7.

EXPLORE ON THE MAP
Navigate Kenya with free OpenStreetMap integration. No API fees, no limits. Tap markers, track visited places, and plan your route.

SAVE YOUR WISHLIST
Heart your favorite destinations and sync them across devices when you sign in with Google or email.

COMMUNITY REVIEWS
Read and share authentic traveler reviews powered by Firebase. Help fellow explorers find the best spots.

MULTI-LANGUAGE SUPPORT
Available in English, Swahili, Arabic, Chinese, French, German, Hindi, Italian, Japanese, Korean, Portuguese, and Spanish.

PREMIUM EXPERIENCE
- Beautiful Jetpack Compose UI with safari-inspired design
- Smooth animations and modern Material 3 interface
- Offline destination data with Room database
- Push notifications for travel updates
- Home screen widget for quick access

Whether you're planning your first safari or returning to Kenya's golden savannahs, Tembea Kenya is your gateway to unforgettable adventures.

Made with love for Kenya.
```

### Category

**Travel & Local**

### Content Rating

Everyone / General audience (no mature content)

### Keywords

Kenya, safari, tourism, travel, Maasai Mara, Diani, Nairobi, Africa, vacation, adventure, AI guide, map, destinations

---

## What's New (v2.4.4)

- Stable release build with R8 minification and resource shrinking
- 120+ curated Kenya destinations with photos and details
- Safari AI Guide for instant travel assistance
- OpenStreetMap integration — free, unlimited mapping
- Firebase-powered community reviews and cloud sync
- Google Sign-In and guest mode
- 12-language localization
- AdMob monetization integration
- Push notifications via Firebase Cloud Messaging
- Home screen widget

---

## Technical Specifications

| Spec | Value |
|------|-------|
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |
| Compile SDK | 35 |
| Language | Kotlin |
| UI Framework | Jetpack Compose, Material 3 |
| Architecture | MVVM |
| Database | Room (offline), Firebase Firestore (cloud) |
| Maps | OSMDroid (OpenStreetMap) |
| AI | OpenAI-compatible engine |
| Ads | Google AdMob |
| Auth | Firebase Auth, Google Sign-In |
| Analytics | Firebase Analytics |

### Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Fetch data, AI, maps, reviews |
| `ACCESS_NETWORK_STATE` | Check connectivity |
| `POST_NOTIFICATIONS` | Push notification delivery |

### Supported Languages

English (default), Swahili, Arabic, Chinese, French, German, Hindi, Italian, Japanese, Korean, Portuguese, Spanish

---

## Google Play Console Checklist

- [ ] Upload `app-release.aab` to Production or Internal Testing track
- [ ] Enroll in Play App Signing (use `upload_certificate.pem` if needed)
- [ ] Add short and full descriptions (see above)
- [ ] Upload screenshots from `Screenshots/` or `docs/assets/`
- [ ] Set app icon (512×512 PNG)
- [ ] Add feature graphic (1024×500)
- [ ] Link privacy policy: `docs/privacy.html` or `PRIVACY_POLICY.md`
- [ ] Complete content rating questionnaire
- [ ] Set target countries/regions
- [ ] Configure AdMob app ID: `ca-app-pub-1281448884303417~5992573219`
- [ ] Verify Firebase `google-services.json` matches production project

---

## Privacy & Compliance

- Privacy policy: `PRIVACY_POLICY.md` / `docs/privacy.html`
- Location: App uses destination coordinates for weather only — does **not** track user GPS
- Ads: Google AdMob (disclose in privacy policy)
- Data: Favorites stored locally; cloud sync optional via sign-in
- Third-party services: Firebase, AdMob, Open-Meteo (weather), OpenStreetMap

---

## Contact & Support

- **Developer:** Gideon Ngeno
- **GitHub:** [@gideongeny](https://github.com/gideongeny)
- **In-app:** Contact Us screen

---

*Document generated for Tembea Kenya v2.4.4 release build — June 24, 2026*
