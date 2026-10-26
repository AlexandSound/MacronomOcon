# Android and Gradle Settings

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
