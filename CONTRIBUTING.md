# Contributing to Grain Kotlin SDK

Thanks for your interest in contributing! Here's how to get started.

## Development Setup

1. Clone the repo and open it in IntelliJ IDEA or Android Studio.
2. Make sure you have JDK 17+ and the Android SDK installed (`local.properties` should point to it).
3. Run `./gradlew :library:jvmTest` to verify everything builds.

## Making Changes

- Fork the repo and create a feature branch from `main`.
- Keep commits focused — one logical change per commit.
- Follow existing code style. The project uses `kotlin.code.style=official`.
- Add tests for new functionality. Unit tests go in `commonTest`, integration tests in `jvmTest`.

## Pull Requests

- Open a PR against `main` with a clear description of what changed and why.
- Make sure all tests pass: `./gradlew :library:jvmTest`
- If your change affects the public API, update the relevant documentation.

## Reporting Issues

Open an issue on GitHub. Include the SDK version, platform (Android/iOS/JVM), and a minimal reproduction if possible.

## Code of Conduct

Be respectful, be constructive, and assume good intent. We're all here to build something useful.
