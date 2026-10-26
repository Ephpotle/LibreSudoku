# Publishing to Google Play

Every push to `main` builds a signed app bundle and uploads it to the **internal testing** track of Google Play,
so phones that installed the app from Play update themselves. The app ID is `com.ephpotle.killersudoku`.

## One-time setup

1. **Create an upload key** on your computer (keep the file and passwords somewhere safe):

   ```sh
   keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 upload.jks > upload.jks.b64   # macOS: base64 -i upload.jks -o upload.jks.b64
   ```

2. **Add repository secrets** under GitHub → Settings → Secrets and variables → Actions:

   | Secret | Value |
   | --- | --- |
   | `ANDROID_SIGNING_KEY` | contents of `upload.jks.b64` |
   | `ANDROID_KEY_ALIAS` | `upload` |
   | `ANDROID_KEYSTORE_PASSWORD` | the keystore password |
   | `ANDROID_KEY_PASSWORD` | the key password (same as above unless you chose another) |

3. **Run the workflow once** (Actions → Publish to Google Play → Run workflow) and download the `release.aab`
   artifact. The Play upload step is skipped until step 5.

4. **Create the app in Play Console**: Create app, then Testing → Internal testing → Create new release, accept
   Play App Signing, and upload `release.aab` by hand. Google requires the first upload to be manual. Add your
   own Google account under Testers, then open the opt-in link on your phone and install the app from Play.

5. **Let GitHub upload future builds**: in Google Cloud, create a service account and a JSON key for it; in Play
   Console → Users and permissions, invite the service account's email with permission to release to testing
   tracks for this app. Save the whole JSON file as the `PLAY_SERVICE_ACCOUNT_JSON` secret.

After that, merging to `main` is all it takes. Play usually delivers an internal testing update within minutes,
and the phone installs it with its normal automatic updates.
