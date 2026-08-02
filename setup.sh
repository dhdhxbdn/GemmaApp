#!/bin/bash
set -e

echo "[+] Создаем структуру Android-проекта..."
mkdir -p .github/workflows
mkdir -p app/src/main/java/com/example/gemmaapp
mkdir -p app/src/main/cpp

# 1. Настройки сборки Gradle (корень)
cat << 'EOT' > build.gradle.kts
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
EOT

cat << 'EOT' > settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOSITORIES)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "GemmaApp"
include(":app")
EOT

# 2. Конфиг модуля приложения с поддержкой C++ (CMake)
cat << 'EOT' > app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.gemmaapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gemmaapp"
        minSdk = 26
        targetSdk = 34
        versionCode 1
        versionName "1.0"

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                arguments("-DANDROID_STL=c++_shared")
            }
        }
        ndk {
            abiFilters.addAll(setOf("arm64-v8a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
}
EOT

# 3. Android Manifest
cat << 'EOT' > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
    
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Gemma 3 Local"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
EOT

# 4. C++ Движок (CMake + Stub Bridge)
cat << 'EOT' > app/src/main/cpp/CMakeLists.txt
cmake_minimum_required(VERSION 3.22.1)
project("gemmaapp")

add_library(
        llama_bridge
        SHARED
        native-lib.cpp
)

find_library(log-lib log)

target_link_libraries(
        llama_bridge
        ${log-lib}
)
EOT

cat << 'EOT' > app/src/main/cpp/native-lib.cpp
#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_gemmaapp_LlamaBridge_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Llama.cpp Engine Ready";
    return env->NewStringUTF(hello.c_str());
}
EOT

# 5. Код на Kotlin (MainActivity + Bridge)
cat << 'EOT' > app/src/main/java/com/example/gemmaapp/LlamaBridge.kt
package com.example.gemmaapp

class LlamaBridge {
    external fun stringFromJNI(): String

    companion {
        init {
            System.loadLibrary("llama_bridge")
        }
    }
}
EOT

cat << 'EOT' > app/src/main/java/com/example/gemmaapp/MainActivity.kt
package com.example.gemmaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bridge = LlamaBridge()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🐉 Gemma 3 Local AI", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Статус C++ движка: ${bridge.stringFromJNI()}")
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { /* Выбор файла */ }) {
                            Text("Выбрать модель Gemma 3 (.gguf)")
                        }
                    }
                }
            }
        }
    }
}
EOT

# 6. Инструкция для автоматической сборки APK на GitHub Actions
cat << 'EOT' > .github/workflows/build.yml
name: Build Android APK

on:
  push:
    branches: [ "master", "main" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout repository
      uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Setup Android SDK
      uses: android-actions/setup-android@v3

    - name: Build APK with Gradle
      run: |
        chmod +x gradlew || true
        gradle build --no-daemon || true
        # Создаем обертку gradle для сборки
        gradle wrapper
        ./gradlew assembleDebug --stacktrace

    - name: Upload APK Artifact
      uses: actions/upload-artifact@v4
      with:
        name: GemmaApp-Debug-APK
        path: app/build/outputs/apk/debug/app-debug.apk
EOT

echo "[+] Готово! Структура проекта развернута."
