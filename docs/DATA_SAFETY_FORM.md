# Google Play Data Safety Form — Tembea Kenya

Use **one URL for all three fields** in Play Console:

```
https://gideongeny.github.io/Kenyatourism/
```

- Privacy policy URL  
- Delete account URL  
- Delete data URL  

Google reviewers can scroll to **Privacy Policy** (`#privacy`) and **Delete Account & Data** (`#delete`) on the same page.

---

## Step 1: Data collection and security

| Question | Answer |
|----------|--------|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** |
| Do you provide a way for users to request that their data is deleted? | **Yes** |
| Delete account / data URL | `https://gideongeny.github.io/Kenyatourism/` |

---

## Step 2: Data types to declare

Check **every row below**. This matches what Tembea Kenya actually collects via AdMob, Firebase, and sign-in.

### Personal info

| Data type | Collected | Shared | Required | Purpose |
|-----------|-----------|--------|----------|---------|
| **Email address** | Yes | No* | Optional (only if user signs in) | Account management, App functionality |
| **Name** | Yes | No* | Optional | Account management, App functionality |
| **User IDs** | Yes | No* | Optional | Account management, App functionality |

\* Shared with **service providers** (Google/Firebase) — in the form, mark **Collected** and under sharing select that data is **not sold** but is processed by third parties as part of app functionality. Firebase is a service provider.

### Photos and videos

| Data type | Collected | Shared | Required | Purpose |
|-----------|-----------|--------|----------|---------|
| **Photos** | Yes | No | Optional | Account management (Google profile photo), App functionality (user-uploaded media) |

### App activity

| Data type | Collected | Shared | Required | Purpose |
|-----------|-----------|--------|----------|---------|
| **App interactions** | Yes | Yes | Not required | Analytics, Advertising |
| **In-app search history** | Yes | No | Not required | App functionality (destination search) |

### App info and performance

| Data type | Collected | Shared | Required | Purpose |
|-----------|-----------|--------|----------|---------|
| **Crash logs** | Yes | No | Not required | Analytics |
| **Diagnostics** | Yes | No | Not required | Analytics |

### Device or other IDs — **THIS CAUSED YOUR REJECTION**

| Data type | Collected | Shared | Required | Purpose |
|-----------|-----------|--------|----------|---------|
| **Device or other IDs** | **Yes** | **Yes** | Not required | **Advertising**, Analytics |

> **Important:** AdMob (`play-services-ads`) sends the **Android Advertising ID** off the device. You must declare **Device or other IDs** as both **collected** and **shared**.

For each data type above, also set:
- **Ephemeral processing:** No  
- **User can request deletion:** Yes (via privacy page)  
- **Optional vs required:** Mark account data as optional; ads/analytics as not required for core app use  

---

## Step 3: How data is used (purposes)

Enable these purposes where applicable:

- **App functionality** — sign-in, favourites sync, reviews, maps  
- **Analytics** — Firebase Analytics  
- **Advertising or marketing** — Google AdMob  
- **Account management** — Firebase Auth  

Do **not** enable "Sold to third parties."

---

## Step 4: Third-party SDK disclosure

In Play Console, when asked about third-party SDKs, disclose:

| SDK | Data it may collect |
|-----|---------------------|
| Google AdMob | Advertising ID, device data, app interactions |
| Firebase Analytics | App instance ID, device info, usage data |
| Firebase Auth | Email, name, user ID |
| Firebase Firestore | Account data, favourites, reviews |
| Google Sign-In | Email, name, profile photo |

Reference: [Google Play SDK Index](https://developer.android.com/distribute/sdk-index)

---

## Step 5: Submit

1. Save the Data safety form  
2. Confirm **Privacy policy URL** matches: `https://gideongeny.github.io/Kenyatourism/`  
3. Go to **Publishing overview** → **Send for review**  

You do **not** need a new APK if you only fix the Data safety form. Your rejection was on **version code 17** for a declaration mismatch, not broken code.

If you upload **version code 18**, ensure the form still matches before submitting.

---

## Why you were rejected

Google's automated scan detected **Device or other IDs** (Advertising ID) transmitted by **AdMob**, but your form said the app does not collect that data type.

**Fix:** Add **Device or other IDs → Collected + Shared → Purpose: Advertising** and align your privacy policy (already updated in `privacy.html`).
