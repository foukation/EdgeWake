# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

This is an Android application built with Java and Kotlin, using Android SDK 34 with minimum SDK 26. The app is an AI assistant platform with multiple modules including chat, AI drawing, meeting transcription, PPT generation, and agent functionality.

## Build System and Commands

- **Build System**: Gradle with Android Gradle Plugin
- **Gradle Version**: 8.10.2 (from gradle-wrapper.properties)
- **Main Module**: `app` (Android application)

### Common Development Commands

1. **Build the project**:
   ```bash
   ./gradlew build
   ```

2. **Assemble debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install debug version on connected device**:
   ```bash
   ./gradlew installDebug
   ```

4. **Run unit tests**:
   ```bash
   ./gradlew test
   ```

5. **Clean build**:
   ```bash
   ./gradlew clean
   ```

6. **Lint check**:
   ```bash
   ./gradlew lint
   ```

## Project Architecture

### Key Components

1. **MVVM Architecture**:
   - ViewModels in `com.fxzs.lingxiagent.viewmodel` packages
   - Activities and Fragments for UI components
   - Data binding patterns

2. **Module Structure**:
   - Authentication (`auth`)
   - Chat functionality (`chat`)
   - AI Drawing (`drawing`)
   - Meetings (`meeting`)
   - PPT generation (`ppt`)
   - User management (`user`)
   - AI Agent system (`agent`)

3. **Core Services**:
   - Network layer using Retrofit2
   - Shared preferences for local storage
   - Image loading with Glide
   - Markdown rendering with Markwon
   - Push notifications with JPush (Jiguang)

4. **Key Features**:
   - AI conversation system
   - Text-to-speech (TTS) functionality
   - Image recognition and processing
   - Document generation (PDF, Word)
   - Meeting transcription and real-time processing
   - PPT outline editing and generation

### Data Models

- DTOs in `model/*/dto` packages
- Repository pattern in `model/*/repository` packages
- API services using Retrofit interfaces

### UI Components

- Fragment-based navigation with bottom tab bar in MainActivity
- RecyclerView adapters for list displays
- Custom views and components in `view` packages
- Floating window services for overlay UI

## Testing

- Unit tests located in `app/src/test/java`
- Uses JUnit 4 for testing framework
- Mockito for mocking dependencies
- Tests organized by feature/viewmodel

## Dependencies

Key libraries include:
- AndroidX components (lifecycle, ViewModel, LiveData)
- Retrofit2 for networking
- Glide for image loading
- RxJava2 for reactive programming
- Markwon for Markdown rendering
- JPush for push notifications
- iText and Apache POI for document generation

## Code Patterns

1. **Base Classes**:
   - `BaseActivity` and `BaseFragment` for common functionality
   - `BaseViewModel` for shared ViewModel behavior

2. **Network Layer**:
   - Retrofit services with RxJava adapters
   - Custom interceptors for authentication
   - API response wrapper classes

3. **Data Management**:
   - Repository pattern for data access
   - SharedPreferences for simple local storage
   - Constants for configuration values

4. **UI Patterns**:
   - RecyclerView with custom adapters
   - Fragment navigation
   - Custom dialogs and UI components

## Important Notes

- The app uses JPush (Jiguang) for push notifications and one-click login
- HighMap API is integrated for location services
- Extensive use of Kotlin alongside Java
- Accessibility service integration for enhanced functionality
- Multiple third-party SDKs integrated (TTS, recognition, etc.)