# QuickCleanPRO Architecture Rules

## Dependency direction

```text
app -> feature presentation -> domain
                      data -> domain
app/feature -> core
core -> core:model
```

- Domain code must not depend on `Context`, `Intent`, `PendingIntent`, Activity, Service, Compose, SDK adapters, or concrete repositories.
- UI code must not read repositories, SDKs, `SharedPreferencesUtils`, Koin `GlobalContext`, or service locators directly.
- ViewModels receive repositories, preferences, dispatchers, and gateways through constructors. Missing DI is a startup/build failure; production code must not fall back to Preview fakes.
- Third-party ad, analytics, remote-config, and attribution APIs stay behind their adapters.

## Feature boundary

- Every ViewModel exposes one immutable `UiState` through `StateFlow`; one-time platform work uses an Effect flow when needed.
- Use a pure `Reducer` only for real multi-stage workflows such as scanning, deleting, cleaning, or virus checks. Display-oriented screens use explicit ViewModel methods without Action/Reducer boilerplate.
- A reducer performs no IO, navigation, SDK call, clock read, or coroutine launch. The ViewModel coordinates IO around it.
- `Route` obtains the ViewModel, collects State/Effect, and performs navigation or platform work.
- `Screen` receives state and callbacks only. Layout is feature-owned and does not need to match other features.
- Koin resolution is restricted to Route/app-host code. Screens do not import repositories, SDK adapters, navigators, or business CompositionLocals.

## Shared code threshold

- Keep UI components inside a feature until at least three callers share the same semantics and interaction.
- Do not add base ViewModels, generic registries, service locators, or wrappers that only forward parameters. Theme tokens are the only supported CompositionLocal service.
- Shared interfaces represent real IO/platform boundaries. Do not create an interface for every class.

## Product configuration

- `config/product.json` is canonical for product identity and ad IDs.
- `config/ad_policy.json` is canonical for area keys and frequency policy.
- APK raw `ad_policy.json` and `native_ad_ids.json` are generated; source copies are forbidden by validation.
- Existing area keys, analytics names, notification aliases, and permission results are compatibility contracts.

## Required checks

Run only checks relevant to the change:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-product-config.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-dependencies.ps1
./gradlew :app:generateProductAdResources
```
