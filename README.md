[![](https://jitpack.io/v/symbiote-h2020/SymbIoTeSecurity4Android.svg)](https://jitpack.io/#symbiote-h2020/SymbIoTeSecurity4Android)

# SymbIoTeSecurity4Android

This is a fork of the [SymbIoTeSecurity](https://github.com/symbiote-h2020/SymbIoTeSecurity) library customized for Android platform.

# Usage
Add to your project build.gradle the jitpack repository as shown below:
```groovy
allprojects {
    repositories {
        google()
        jcenter()
        maven { url 'https://jitpack.io' }
    }
}
```

Add to your build.gradle in the app module:

```groovy
dependencies {
  implementation 'com.github.symbiote-h2020:SymbIoTeSecurity4Android:27.2.0'
}
```

```groovy
 android {
    ...
    compileOptions {
        sourceCompatibility 1.8
        targetCompatibility 1.8
    }
```

# Example use case
There is sample app module with [activity](https://github.com/symbiote-h2020/SymbIoTeSecurity4Android/blob/master/sampleapp/src/main/java/symbiote/h2020/eu/sampleapp/MainActivity.java) where you find a step by step guidance how to get SecurityRequest
