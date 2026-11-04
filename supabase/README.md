# Supabase backend

Project: `vakuttlbmvqqtklapovu` (Macronomicon)

API URL: `https://vakuttlbmvqqtklapovu.supabase.co`

The initial SQL migration was applied through the hosted SQL Editor on November 3,
2026. All four tables and their owner policies were verified in the database catalog.
This does not constitute an end-to-end Android or cross-user access test.

Tables:

- `profiles`: display name and preferences. Client upserts its own profile after sign-in.
- `pantry_items`: user-entered stock with quantity, unit, expiration, and barcode.
- `recipe_bookmarks`: favorites/blocked flags and permitted recipe references.
- `custom_recipes`: recipes authored by the user.

All tables use Supabase Auth user IDs and enforce owner-only reads and writes for
authenticated clients. Anonymous access is revoked. Email/password and MFA belong
to Supabase Auth, not these tables. No trigger is required to create a profile.

The migration was applied manually, so CLI migration history has not been marked.
Before a future CLI push, authenticate, link, and reconcile the existing migration:

```sh
supabase login
supabase link --project-ref vakuttlbmvqqtklapovu
supabase migration repair 20261104010000 --status applied
supabase db push --dry-run
```

Do not reapply the initial migration to the hosted database. It is for reproducing
the schema in a new environment. `config.toml` controls local development; it does
not prove hosted Auth settings were changed.

## Spoonacular Edge Function

`functions/spoonacular/index.ts` is the only place the Spoonacular key is used. It requires a
signed-in user's access token, forwards six allowlisted actions (`search`,
`findByIngredients`, `recipe`, `recipes` (bulk, up to 20 IDs), `ingredients`, `upc`) with
bounded inputs and at most 10 results, and maps an exhausted daily quota (Spoonacular 402) to
429. Responses are not stored server-side. The Android `SpoonacularApiClient` calls it through
`supabase.functions`.

Quota: the Free plan allows 50 points/day and 1 request/second; a recipe search with
ingredients and instructions costs about 1.6 points. `CachingRecipeApiClient` keeps results in
memory for up to an hour (Spoonacular allows only temporary caching), runs recipe calls one at
a time so Kitchen and Recipes share one search, reuses search results when a recipe is opened,
and loads saved recipes with one bulk call. Redeploy the function after changing it.

Set the key as a project secret (never in `local.properties` or the APK), then deploy:

```sh
supabase login
supabase link --project-ref vakuttlbmvqqtklapovu
supabase secrets set SPOONACULAR_API_KEY=your_spoonacular_key
supabase functions deploy spoonacular --no-verify-jwt
```

Dashboard alternative for the secret: Edge Functions → Secrets → add
`SPOONACULAR_API_KEY`. `--no-verify-jwt` matches `config.toml`: the function checks the
session itself with `auth.getUser`, which works with publishable keys. `supabase secrets list`
shows the secret's name and digest only. After deploying, sign in on the app and open Recipes.

Favorites and blocks use `recipe_bookmarks`; Spoonacular recipe IDs are stored, not payloads.

Remaining integration:

- Pantry and comments are in memory for the running app and start empty. Connect
  `pantry_items` once meal deductions run atomically on the server, and add a comments table.
- Barcode lookup uses Spoonacular's UPC endpoint and then ingredient search; Open Food Facts
  can replace or supplement it.
- Spoonacular's free plan requires attribution; add it before publishing the app.
- Test two separate accounts to confirm read/write isolation and account deletion.

Only store user-authored pantry/custom recipe data here. This schema does not grant
permission to retain restricted Spoonacular payloads or derived data.

## Android authentication

The application now uses Supabase email/password accounts on a dedicated entry screen. Signed-out users cannot enter the main
app; signing out from Profile returns to that screen.
Add the client key to the ignored root `local.properties` file, preserving `sdk.dir`:

```properties
SUPABASE_PUBLISHABLE_KEY=sb_publishable_your_key
```

Rebuild after changing it. Never use a secret or service-role key. Without a client
key the form is disabled and identifies the missing configuration.

Registration supports email confirmation: open the confirmation email, then return
and sign in with the password. Automatic sign-in is also handled when confirmation
is disabled on the server.

### Google sign-in

"Continue with Google" uses Supabase OAuth with PKCE. The app opens Google in a Custom
Tab, Supabase redirects to `macronomocon://login-callback`, and `MainActivity`
(`singleTask`) hands that intent to `AuthAppContainer.handleAuthRedirect`, which
exchanges the code for a session. `AccountViewModel` then loads the profile and the
entry screen opens the app. Google creates the Supabase user on first use.

One-time hosted setup (not done by code in this repository):

1. Google Cloud Console → APIs & Services → Credentials → Create OAuth client ID,
   type **Web application**. Add the authorized redirect URI
   `https://vakuttlbmvqqtklapovu.supabase.co/auth/v1/callback`.
2. Supabase dashboard → Authentication → Sign In / Providers → Google: enable it and
   paste that Web client ID and secret.
3. Supabase dashboard → Authentication → URL Configuration → Redirect URLs: add
   `macronomocon://login-callback`.

No Android OAuth client or SHA-1 fingerprint is needed for this browser flow. For
the local stack, export `SUPABASE_AUTH_EXTERNAL_GOOGLE_CLIENT_ID` and
`SUPABASE_AUTH_EXTERNAL_GOOGLE_SECRET` before `supabase start`.

This version does not handle password recovery,
MFA enrollment/challenges, or account deletion. Do not enable MFA for demo accounts
until a challenge screen is implemented. Hosted Auth settings were not changed.

The Supabase SDK restores and refreshes sessions. Session preferences are excluded
from Android cloud backup and device transfer. Passwords are transient Compose state
and are cleared on submission. Profile preferences use the protected `profiles` table.
Food features still use the existing demo session, and the Profile screen says so.
The default mock container remains available to previews and existing UI tests.

Validation should include confirmation-required registration, invalid credentials,
app restart after sign-in, sign-out, and separate users updating their own profiles.
