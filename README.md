This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

### POC 3: Offline emergency continuity

The offline emergency POC does not use a local LLM, NLP, MLKit, Gemini Nano, or any real AI model. It validates conversational continuity through a deterministic local decision tree inside the Chat experience.

When cloud chat fails with `offline` or `connectionLost`, Chat starts inline offline support automatically. For `timeout` and `error`, Chat offers offline support without cancelling the cloud retry path. The local flow loads hardcoded emergency guides, contacts, guided questions, local `pendingSync` reports, and simulated sync through shared use cases and `LocalCacheStore`.

This represents IA-12: offline emergency assistance. The cloud experience can fail, but the conversational channel keeps a minimum local support path available.

### Native SDK Integration Spike

This spike simulates a future native conversational SDK integration, such as Kore.ai, Botmaker, or another platform SDK. It does not integrate any real SDK.

The shared layer defines `ChatProviderAdapter`, and `NativeSdkAdapter` is implemented with `expect/actual` for Android and iOS. Each platform actual delegates to a fake native SDK that simulates `onStarted`, `onToken`, `onCompleted`, and `onError` callbacks. The provider `providerSdk` routes Chat through:

`Shared ViewModel -> UseCase -> ChatService -> ChatProviderAdapter -> NativeSdkAdapter -> FakeNativeSdk`

This validates encapsulation, provider replacement cost, streaming callback mapping, and metrics such as `sdkTimeToFirstToken` and `sdkTotalResponseTime` without contaminating shared UI or domain contracts.

### Local AI Readiness Spike

This spike does not implement real local AI, local inference, MLKit, Gemini Nano, or any model runtime.

The shared layer defines `LocalAiCapability`, `LocalAiRequest`, and `LocalAiResult`, with `NativeLocalAiCapability` implemented through `expect/actual` on Android and iOS. Platform actuals delegate to fake emergency assessment engines. When `PocConfig.enableLocalAiReadiness` is enabled, the offline emergency flow can execute the local capability after the guided questions and show a deterministic summary, next step, and confidence estimate.

This validates architectural readiness for future offline agents through:

`Shared ViewModel -> UseCase -> LocalAiCapability -> NativeLocalAiCapability -> FakeEmergencyAssessmentEngine`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
