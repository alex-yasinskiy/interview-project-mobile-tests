### BUG-01: App unexpectedly re-opens on the Main Screen after pressing system `Back` on the Splash Screen
* **Severity:** Medium
* **Environment:** Android 14 (API 34), Pixel 7 Emulator, App v1.0 (`com.zipo.interviewproject`)
* **Steps to Reproduce:**
  1. Cold-start the application (`SplashActivity` is displayed).
  2. Immediately press the system `Back` button while the loading spinner is still visible.
  3. Wait 2–3 seconds on the device home screen.
* **Expected Result:** The app closes and remains closed in the background; the user stays on the home screen.
* **Actual Result:** The app closes briefly, then automatically launches again and opens the Main Screen (`Earth 1..N` list) over the home screen.
* **Likely Root Cause:** Uncancelled delayed transition task (`Handler.postDelayed` / coroutine) in `SplashActivity.onDestroy()`.

---

### BUG-02: Horizontal misalignment of `DOWNLOAD` buttons on items with two-digit numbers (`Earth 10+`)
* **Severity:** Low
* **Environment:** Android 14 (API 34), Pixel 7 Emulator (1080x2400), App v1.0
* **Steps to Reproduce:**
  1. Launch the app and wait for the Main Screen to load.
  2. Scroll down to view `Earth 9`, `Earth 10`, and `Earth 11` simultaneously.
* **Expected Result:** All `DOWNLOAD` buttons are aligned along the same vertical axis regardless of label length.
* **Actual Result:** Starting from `Earth 10`, the `DOWNLOAD` button shifts noticeably to the right compared to `Earth 1`–`Earth 9`.