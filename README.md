# gradle-android-jawa-plugin

gradle-android-jawa-plugin adds jawa language support to official gradle android plugin.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Installation](#installation)
  - [1. Add buildscript's dependency](#1-add-buildscripts-dependency)
  - [2. Apply plugin](#2-apply-plugin)
  - [3. Add scala-library dependency](#3-add-scala-library-dependency)
  - [4. Put scala source files](#4-put-scala-source-files)
- [Changelog](#changelog)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Installation

### 1. Add buildscript's dependency

`build.gradle`
```groovy
buildscript {
    dependencies {
        classpath "com.android.tools.build:gradle:2.0.0"
        classpath "com.github.arguslab.gradle:gradle-android-jawa-plugin:1.0"
    }
}
```

### 2. Apply plugin

`build.gradle`
```groovy
apply plugin: "com.android.application"
apply plugin: "org.argus.android-jawa"
```

### 3. Put jawa source files

Default locations are src/main/jawa.
You can customize those directories similar to java.

`build.gradle`
```groovy
android {
    sourceSets {
        main {
            scala {
                srcDir "path/to/main/jawa" // default: "src/main/jawa"
            }
        }
    }
}
```

## Changelog
- 1.0 First release
