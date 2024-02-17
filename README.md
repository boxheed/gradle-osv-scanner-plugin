# Gradle OSV-Scanner Plugin

The Gradle OSV Scanner Plugin is a convenient integration of the OSV Scanner, a standalone command-line tool for scanning open source dependencies for known vulnerabilities. This plugin simplifies the process of running vulnerability scans on your project's dependencies directly from your Gradle build scripts.

## Features

Easy integration of the OSV Scanner into your Gradle-based projects.
Customizable scan configurations to fit your project's needs.
Seamless integration with your CI/CD pipelines for automated vulnerability scanning.

## Installation

Add the following snippet to your build.gradle file to apply the OSV Scanner plugin:

```groovy
plugins {
    id 'com.fizzpod.osv-scanner' version '0.0.1'
}
```

Or for older Gradle versions:

```groovy
buildscript {
    repositories {
        maven {
            url 'https://plugins.gradle.org/m2/'
        }
    }
    dependencies {
        classpath 'com.fizzpod:gradle-osv-scanner-plugin:0.0.1'
    }
}

apply plugin: 'com.fizzpod.osv-scanner'
```

## Usage

### Installation of osv-scanner
You can use the `osvInstall` task to download and install osv-scanner. By default, this will install the latest version of osv-scanner in the `build/osv-scanner` folder.

```bash
./gradlew osvInstall
```

### Running a scan
You can use the `osvScan` task to initiate a vulnerability scan. By default, the scan will analyze all dependencies declared in your project, however note that for gradle projects you need to create lock files. 
See [Locking dependency versions](https://docs.gradle.org/current/userguide/dependency_locking.html) in the Gradle documentation

```bash
./gradlew osvScan
```

## License
This project is licensed under the [Apache 2.0 License](https://spdx.org/licenses/Apache-2.0.html).