# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Episodive is an Android podcast application built with Kotlin and Jetpack Compose that uses the Podcast Index API. The app follows modern Android development practices with a multi-module architecture using Gradle convention plugins.

## Build Commands

### Core Commands
- `./gradlew build` - Build all modules
- `./gradlew test` - Run unit tests for all modules
- `./gradlew check` - Run all verification tasks (tests, lint, etc.)
- `./gradlew testDebugUnitTest` - Run debug unit tests only
- `./gradlew connectedAndroidTest` - Run instrumentation tests on connected devices

### Module-specific Testing
- `./gradlew :core:database:test` - Test database module only
- `./gradlew :core:network:test` - Test network module only
- `./gradlew :core:data:test` - Test data module only
- `./gradlew :core:domain:test` - Test domain module only
- `./gradlew :feature:home:test` - Test home feature module
- `./gradlew :feature:search:test` - Test search feature module

### Code Quality
- `./gradlew lint` - Run lint analysis
- `./gradlew lintFix` - Auto-fix lint issues where possible

### Coverage
- `./gradlew createDebugCoverageReport` - Generate test coverage reports (uses Jacoco)

## Architecture

### Module Structure

The project follows a clean architecture approach with the following modules:

**Core Modules:**

- **`:core:model`** - Domain models and data classes (Podcast, Episode, Category, UserData, etc.)
- **`:core:domain`** - Business logic interfaces and repository contracts
- **`:core:data`** - Repository implementations coordinating between network, database, and
  datastore
- **`:core:network`** - Remote data sources using Retrofit for Podcast Index API
- **`:core:database`** - Local storage using Room database with DAOs and entities
- **`:core:datastore`** - User preferences using DataStore (onboarding state, categories)
- **`:core:player`** - Audio playback using ExoPlayer
- **`:core:designsystem`** - Shared UI components and theming
- **`:core:testing`** - Shared test utilities and test data

**Feature Modules:**

- **`:feature:onboarding`** - Onboarding flow with category selection
- **`:feature:home`** - Main home screen with podcast feeds
- **`:feature:search`** - Search and browse podcasts
- **`:feature:library`** - User's saved and liked episodes
- **`:feature:podcast`** - Podcast details and episodes
- **`:feature:player`** - Audio player UI
- **`:feature:clip`** - Soundbites and clips

**App Module:**

- **`:app`** - Main Android application with navigation and dependency integration

### Key Architecture Patterns

**Data Flow**: Remote API → Network DataSource → Repository → Domain → UI
**Local Storage**: Room Database with TypeConverters for complex types
**User Preferences**: DataStore for lightweight user settings
**Audio Playback**: ExoPlayer integration in `:core:player` module
**Dependency Injection**: Hilt for DI across all modules
**Testing**: Robolectric for database tests, MockK for mocking, Turbine for Flow testing

### Build Logic
Uses Gradle convention plugins in `build-logic/convention/` for consistent module configuration:

- `episodive.android.application` - Application module setup
- `episodive.android.application.compose` - Compose setup for app module
- `episodive.android.application.jacoco` - Coverage for app module
- `episodive.android.library` - Standard Android library setup
- `episodive.android.library.compose` - Compose setup for libraries
- `episodive.android.library.jacoco` - Coverage for libraries
- `episodive.android.feature` - Feature module template (combines library, compose, hilt, jacoco,
  test)
- `episodive.android.room` - Room database configuration
- `episodive.android.hilt` - Hilt dependency injection
- `episodive.android.test` - Test dependencies and configuration

### Important Implementation Details

**Enum Handling**: The `Medium` enum uses value properties (e.g., `Podcast("podcast")`) and requires
custom TypeConverters in Room that convert using `entries.find { it.value == stringValue }` rather
than `valueOf()`. The same pattern applies to `EpisodeType`, `Category`, and other enums.

**Database Entities**: Main entities include `PodcastEntity`, `EpisodeEntity`, and user interaction
entities like `FollowedPodcastEntity`, `LikedEpisodeEntity`, `PlayedEpisodeEntity`, etc. Entities
are cached with timestamps (`cachedAt`) and cache keys (`cacheKey`) for efficient data management.

**API Integration**: Uses Retrofit with custom interceptors for the Podcast Index API. Response
wrappers (`ResponseWrapper`, `ResponseListWrapper`) provide consistent API response handling.

**User Preferences**: Uses DataStore Preferences for storing user settings like `isFirstLaunch`,
selected `categories`, and `language`.

**Player Integration**: The `:core:player` module wraps ExoPlayer and exposes Flow-based APIs for
playback state (`nowPlaying`, `progress`, `isPlaying`, `playlist`, etc.).

## Development Workflow

When working with this codebase:

1. **Making Database Changes**: Always update both the entity and corresponding DAO, and add/update TypeConverters as needed
2. **Adding New Endpoints**: Create response models in `:core:network`, implement in API interfaces, then add to data sources
3. **Testing Database Code**: Use the existing test data classes in `:core:testing` and follow the Robolectric + Turbine pattern for DAO testing
4. **Repository Pattern**: Always implement repository interfaces in `:core:domain` and provide implementations in `:core:data`
5. **Adding New Features**: Use the `episodive.android.feature` convention plugin which
   automatically sets up compose, hilt, testing, and jacoco

## Test Data

The `:core:testing` module provides test data factories:

- `PodcastTestData` - Sample podcast data (10 podcasts)
- `EpisodeTestData` - Sample episode data (10 episodes)
- `FeedTestData` - Sample feed data (trending, recent, recent new, soundbites)

Use these for consistent test data across modules rather than creating inline test objects.
