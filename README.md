# Gold Time Co. - Full Stack (Android + ASP.NET Core API + Firebase)

Three tiers, matching the Service-Oriented Architecture in the project
document:

```
 Android app (Kotlin, Jetpack Compose)
        |  HTTPS/JSON (Retrofit)
        v
 GoldTime.Api (ASP.NET Core Web API)
        |  Firebase Admin SDK / Firestore        |  static files
        v                                        v
 Firebase (Authentication, Firestore)      wwwroot/images/ (product photos)
```

The Android app **never talks to Firebase directly** - it only calls
`GoldTime.Api`. The API is the only thing that holds Firebase credentials,
verifies passwords, issues/validates ID tokens, and serves product images.
This mirrors the Kerberos Vault-style layered setup: frontend -> API ->
backend service, with the backend being the only tier trusted with secrets.

Folders:
- `GoldTime-Api/` - the ASP.NET Core Web API (backend)
- `GoldTime-Android/` - the Kotlin/Compose app (frontend)

---

## 1. Set up Firebase (once)

### 1.1 Create the project
1. https://console.firebase.google.com -> **Add project** -> name it (e.g.
   `gold-time-co`).

### 1.2 Enable Email/Password sign-in
1. **Build -> Authentication -> Get started**.
2. Under **Sign-in method**, enable **Email/Password** only (no Google SSO).

### 1.3 Create Firestore
1. **Build -> Firestore Database -> Create database** -> test mode for
   development -> pick a region.
2. Two collections will be used: `users/{uid}` (written automatically by the
   API on register) and `products/{productId}` (you seed this - see 1.5).

### 1.4 Seed a product document
Product images are **not** stored in Firebase - they're served directly by
`GoldTime.Api` from its own `wwwroot/images/` folder (see section 2).

In Firestore, start a `products` collection and add a document with fields:

| Field | Type | Example |
|---|---|---|
| name | string | `1/2 oz Gold Krugerrand` |
| description | string | `Gold bullion coin` |
| price | number | `18500` |
| imagePath | string | `gold-krugerrand.jpg` |

`imagePath` is just a filename - the API turns it into a full URL
(`http://<api-host>/images/gold-krugerrand.jpg`) before sending it to the app.
Until you add real products, Home falls back to the four demo names from the
mock with empty image placeholders.

### 1.5 Get the two values the API needs
1. **Project Settings (gear icon) -> General** -> copy the **Project ID**.
2. Still in General, scroll to **Your apps** -> if none exists, click the
   **</>** (Web) icon to register a placeholder web app - this is only to
   obtain a **Web API Key**, shown right after registration (also visible
   later under Project Settings -> General -> Web API Key).
3. **Project Settings -> Service accounts -> Generate new private key** ->
   download the JSON file.

---

## 2. Set up the API (`GoldTime-Api`)

### 2.1 Prerequisites
- .NET 8 SDK (https://dotnet.microsoft.com/download)
- Visual Studio 2022 or `dotnet` CLI

### 2.2 Add your Firebase credentials
1. Copy the service account JSON you downloaded in step 1.5 into
   `GoldTime-Api/firebase-service-account.json` (exact filename).
2. Open `GoldTime-Api/appsettings.json` and fill in:
   ```json
   "Firebase": {
     "ProjectId": "gold-time-co-xxxxx",
     "WebApiKey": "AIzaSy...",
     "ServiceAccountPath": "firebase-service-account.json"
   }
   ```

### 2.3 Add product images
Drop image files straight into `GoldTime-Api/wwwroot/images/`, e.g.
`gold-krugerrand.jpg`. That filename is exactly what you put in each
product's `imagePath` field in Firestore (step 1.4) - no folders, no full
URL, just the filename. The API builds the full URL itself and serves the
file publicly via ASP.NET Core's static file middleware.

### 2.4 Run it
```bash
cd GoldTime-Api
dotnet restore
dotnet run
```
This starts the API at `http://localhost:5000` and opens Swagger UI
(`/swagger`) so you can try `POST /api/auth/register`, `POST
/api/auth/login`, and `GET /api/products/featured` (paste the returned
`idToken` into Swagger's **Authorize** button as `Bearer <token>` to call the
products endpoint) before ever touching the Android app.

### 2.5 Endpoints
| Method | Route | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | none | Creates the Firebase user, writes `users/{uid}` to Firestore, returns a session |
| POST | `/api/auth/login` | none | Verifies email/password, returns a session |
| POST | `/api/auth/forgot-password` | none | Sends a Firebase password-reset email |
| GET | `/api/products/featured?limit=4` | Bearer token | Featured Assets, images pre-resolved |

---

## 3. Set up the Android app (`GoldTime-Android`)

1. Open `GoldTime-Android/` in Android Studio and let Gradle sync (no
   Firebase config file needed anymore - the app has no Firebase dependency
   at all).
2. Check `app/src/main/java/com/goldtimeco/app/network/RetrofitClient.kt`:
   - Running the API locally + Android **emulator** -> leave
     `BASE_URL = "http://10.0.2.2:5000/"` as-is (`10.0.2.2` is the emulator's
     alias for your machine's `localhost`).
   - Running on a **physical device** on the same Wi-Fi -> change it to your
     machine's LAN IP, e.g. `"http://192.168.1.50:5000/"`.
   - Deploying the API somewhere real -> change it to that HTTPS URL, then
     delete `res/xml/network_security_config.xml` and its reference in
     `AndroidManifest.xml` (that file only exists to allow plain HTTP during
     local development).
3. Make sure `GoldTime-Api` is running (`dotnet run`), then **Run ▶** the
   Android app on an emulator (API 26+) or device.
4. You should land on Login. Tap **CREATE ACCOUNT**, fill in the form,
   submit - the app calls the API, the API creates the Firebase user and
   signs them in, and you land on Home with the token stored for next time.

---

## 4. What changed from the Firebase-direct version

- The Android app no longer includes the Firebase SDK, `google-services.json`,
  or the `com.google.gms.google-services` Gradle plugin.
- `AuthRepository` and `ProductRepository` on Android now call
  `GoldTime.Api` via Retrofit instead of calling Firebase Auth/Firestore/Storage
  directly.
- The Firebase ID token issued by the API is stored locally via
  `SessionManager` (SharedPreferences) and sent as `Authorization: Bearer
  <token>` on every request to a protected endpoint.
- All Firebase secrets (service account key, Web API key) now live only on
  the API - nothing sensitive ships inside the Android app.
- Product images are served from the API's own `wwwroot/images/` folder
  (plain ASP.NET Core static files) rather than Firebase Storage - one less
  Firebase service to configure, and images are as simple to update as
  dropping a file into that folder.

## 5. Before going live

- Lock down Firestore security rules (test-mode rules expire and are wide
  open) - since the API talks to Firestore using the admin service account,
  it bypasses these rules anyway, so tightening them mainly protects against
  anyone who got a client SDK talking to Firebase directly in future.
- Deploy `GoldTime-Api` behind HTTPS (e.g. an App Service, a VM, or a
  container host) and point the Android app's `BASE_URL` at it.
- Move `Firebase:WebApiKey` and the service account file out of
  `appsettings.json`/source control and into your hosting platform's secret
  manager or environment variables for production.
