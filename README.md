# Android and Gradle Settings

## Class diagram implementation

The launcher opens a working Compose demo with Kitchen, Pantry, Recipes, and Profile
bottom-navigation tabs. Recipe details nest ingredient lists, instructions, meal
confirmation, and comments; pantry entry uses a child dialog with a demo barcode
lookup and manual search. Warm light/dark themes include hover and press feedback.

### Design patterns

| Pattern | Where |
| --- | --- |
| MVVM | `ui/*/…ViewModel` expose read-only `StateFlow<ViewState<T>>`; Compose screens render it and send events back |
| Repository Singleton | `MockAppContainer.getInstance` (double-checked, `@Volatile`) gives the process one container, so every screen shares one instance of each repository |
| Factory | `AppContainer.viewModelFactory()` builds every ViewModel; `AppContainer.barcodeScanner()` creates scanners; `PantryItemFactory` turns raw form input into a valid `PantryItem` |
| Adapter | `BarcodeScanner` is the target interface; `MLKitBarcodeScanner` adapts ML Kit's callback client and `DemoBarcodeScanner` adapts a fixture code. `IngredientApiClient`/`RecipeApiClient` are the targets for future transport adapters |
| Dependency injection | Constructor injection from the composition root; no child view looks up a dependency |

Fixed values such as units, the "use soon" window, the quick-recipe limit, the maximum
shelf life, and demo IDs are `const val`s in companion objects on the class they belong to.

### Dependency injection and demo data

`MacronomoconApplication` owns one `AppContainer`. The current binding is
`MockAppContainer.getInstance(resources)`; `AppContainer.viewModelFactory()` injects
repositories into lifecycle-owned ViewModels. Compose collects their StateFlows
with `collectAsStateWithLifecycle`. Replace the container binding with production
repository implementations when the backend is ready; screens do not access mocks.
This uses explicit constructor injection, with no service locator inside child views.

The mock container shares one mutex-protected in-memory session. Adding/removing
stock, recording meals, saving recipes, changing preferences, and posting/liking/deleting
comments update that session across screens. Independent containers remain isolated
for tests (their constructor is `internal` and `@VisibleForTesting`). Restarting the app
process resets demo data. Mock authentication and share
URLs are fixtures, not production services. The UI clearly labels demo mode and does
not claim to send notifications or use the camera.

Sample display content is in `res/values/demo_strings.xml`, and all new UI copy is in
`res/values/ui_strings.xml`. `MockSeed` can also be constructed directly in unit tests.
`KitchenPreviews.kt` provides light/dark Android Studio previews.

Parents own their children's state: `MacronomoconApp` decides which screen, top bar, and
dialog are shown and wires every callback to a ViewModel; each screen lays out its own
child rows; children such as `CommentComposer` and `IngredientQuantityFields` are
stateless and receive values and callbacks. The add-ingredient dialog sends raw input
up, and `PantryViewModel.submitStock` validates it through `PantryItemFactory`.

Images are Material Icons vector drawables only (`res/drawable/ic_*.xml`); recipes show
`RecipeSymbol` until real photos exist. The app ships no generated artwork.

### Model and MVVM foundation

| Package | Implemented responsibilities |
| --- | --- |
| `domain/model` | Ingredients, recipes, pantry stock/expiration/deduction, profiles, preferences, comments, and enums |
| `data/repository` | Ingredient and recipe orchestration; authenticated account, pantry, comment, and recipe-storage contracts |
| `data/api` | Ingredient lookup/search and recipe API contracts for transport implementations |
| `data/scanner` | Google ML Kit adapter for EAN/UPC barcode decoding on the device |
| `ui/account`, `ui/pantry`, `ui/recipe`, `ui/comment` | ViewModels with read-only StateFlow, serialized actions, loading/error state, and cancellation handling |

`PantryViewModel` receives a `BarcodeScanner` from the container, closes it when the
ViewModel is cleared, and `scanBarcode()` passes the decoded string to the same lookup
as `identifyBarcode`. Missing or invalid codes,
unmatched products, and lookup failures enable ingredient search. Selecting a result
does not modify the pantry until the user confirms a quantity, unit, and expiration.
`RecipeViewModel.findRecipes` sends selected ingredients through `RecipeRepository`.
API interfaces deliberately have no fake production implementation or embedded keys.

`RecipeIngredient` adds quantity and unit information that the diagram's ingredient
association leaves implicit. Pantry deductions consume the earliest-expiring stock
first, exclude expired stock, and leave all stock unchanged if a requirement cannot
be met. Units are matched without guessing conversions. An expiration date is valid
through that date. Domain dates accept an explicit date for deterministic tests.

Application-owned UI text lives in `app/src/main/res/values/strings.xml`.
ViewModel errors use `UiMessage`; Compose views call `errorMessage.resolve()` when
rendering so text follows the current locale. Never display exception messages.
Domain validation messages are developer diagnostics, not UI text.

IDs, barcodes, units, email addresses, passwords, OTP codes, and URLs use ordinary
Kotlin strings throughout models, repository contracts, ViewModels, and mock data.
Validation stays in the existing model or repository operations. Pantry unit matching
ignores case and surrounding whitespace without guessing conversions. Finite choices
such as difficulty, meal type, navigation destinations, and UI messages remain enums.

Profiles and comment updates return copies. The Android `User` contains no password
hash: password verification, OTP challenges, lockouts, and authorization belong to
the authentication service. Server repository implementations must derive identity
from the session, enforce ownership/unique likes, and make inventory writes atomic.

Remaining integration work: authenticated backend transports, Open Food Facts and
Spoonacular transport mapping, production authentication screens, and live camera
capture/permission handling. A camera binding should return
`MLKitBarcodeScanner { nextFrame() }` from `barcodeScanner()` and retain each frame
until `scan()` returns.
The singleton connection pool, connection factory, and MySQL connections belong in
a separate backend module; this Android-only project does not connect directly to MySQL.

Run the implementation checks with:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew ktlintCheck :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

With an emulator running, execute the Compose navigation test using
`./gradlew :app:connectedDebugAndroidTest`. It covers demo ingredient entry, bottom
tabs, nested recipe details, back navigation, and saved recipes.

The ViewModels follow the [Android architecture guidance](https://developer.android.com/topic/architecture/recommendations).
The scanner uses the [bundled ML Kit barcode model](https://developers.google.com/ml-kit/vision/barcode-scanning/android).
Core library desugaring supports `java.time` on the project's API 24 minimum.

| Setting | Version |
| --- | --- |
| Compile SDK | 37 |
| Target SDK | 37 |
| Minimum SDK | 24 |
| Gradle | 9.6.0 |
| Android Gradle Plugin (AGP) | 9.4.1 |
| Gradle daemon JDK | 25 |
| Java source/target compatibility | 11 |
| Kotlin Compose plugin | 2.2.10 |
| Compose BOM | 2026.02.01 |

## Kotlin formatting (ktlint)

After changing Gradle configuration, select **File > Sync Project with Gradle Files**
in Android Studio. In the Gradle tool window, run `ktlintCheck` to check formatting
or `ktlintFormat` to fix it. You can also run these from the project terminal:

```sh
./gradlew ktlintCheck
./gradlew ktlintFormat
```

The root tasks cover Kotlin Gradle scripts and the app's Kotlin sources, including
tests. Formatting rules live in `.editorconfig` and use the `android_studio` style
with Compose function names allowed. The Gradle `check` task includes ktlint checks.

If the terminal cannot find Java on macOS, use Android Studio's bundled runtime:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew ktlintCheck
```
