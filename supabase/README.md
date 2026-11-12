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

These initial tables use Supabase Auth user IDs and enforce owner-only reads and writes for
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

## Shared recipe comments

Apply `migrations/20261112010000_recipe_comments.sql` to the hosted project before
running the updated Android app. This is a new migration; do not rerun the initial
migration. In the Supabase SQL Editor, run the complete contents of the new file
once. For CLI deployment, reconcile the initial migration history as described
above, inspect `supabase db push --dry-run`, then run `supabase db push`. If applying
the comments migration manually, mark `20261112010000` applied before a later CLI push.

- `comments` stores the recipe reference, author, display-name snapshot, text,
  optional image URL, and server timestamp. Signed-in users can read all comments;
  only the author can publish/delete their own. No client updates are allowed.
- `comment_likes` stores one row per comment/user. RLS rejects impersonation and
  self-likes; the composite primary key rejects duplicate likes. Deleting a comment
  cascades to its likes. Anonymous clients cannot read or write either table.
- The insert trigger reads the author's own profile under RLS. Clients cannot supply
  author names, comment IDs, or timestamps. Missing names use the localized UI fallback.
- Text, an image URL, or both satisfy the SRS content rule. Photo uploading, image
  rendering, and storage-object cleanup remain the separate comment-photos task.
- `SupabaseAccountRepository` (wired in `AuthAppContainer`) reads and writes these tables;
  `AccountViewModel` keeps the loaded comments on the signed-in `User`. Sign-out does not
  delete comments. Reopening a recipe reloads shared comments/likes;
  this implementation does not subscribe to realtime updates.

Recipe IDs reference the external recipe catalog, as bookmarks already do, rather
than a local copy of provider recipes. Custom recipe deletion is not implemented;
its future backend flow must also remove that recipe's comments to honor composition
in the class diagram.

Validation:

1. Run the Android unit tests (including `CommentMapperTest`), debug build, and lint.
2. On a disposable Supabase database with both migrations applied, execute
   `psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/comments_rls.sql`.
   The script tests shared reads, ownership, spoofing, self-likes, duplicate likes,
   empty content, image-only content, cascade deletion, and anonymous denial using
   real database roles. All fixtures are rolled back. It requires a database admin
   connection, not the app's publishable key.
3. With accounts A and B, have A post a comment. Restart the app and open the same
   recipe as B; verify A's comment is visible and B can like but cannot delete it.
   Reopen as A; verify the like count, disabled self-like button, and deletion.
   Reopen as B and verify the deleted comment is gone.

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

- Pantry items persist to `pantry_items` through `SupabaseAccountRepository`. Recording a
  meal writes each changed item separately; move deductions to an atomic server operation.
- Barcode lookup uses Spoonacular's UPC endpoint and then ingredient search; Open Food Facts
  can replace or supplement it.
- Spoonacular's free plan requires attribution; add it before publishing the app.
- Test two separate accounts to confirm read/write isolation and account deletion.

Only store user-authored pantry/custom recipe/comment data and permitted references here. This schema does not grant
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
Pantry items use `pantry_items`; recipes use Spoonacular and comments use the shared Supabase tables.
The default mock container remains available to previews and existing UI tests.

Validation should include confirmation-required registration, invalid credentials,
app restart after sign-in, sign-out, and separate users updating their own profiles.
