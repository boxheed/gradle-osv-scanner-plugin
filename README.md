[![CircleCI](https://circleci.com/gh/boxheed/gradle-osv-scanner-plugin/tree/main.svg?style=shield)](https://circleci.com/gh/boxheed/gradle-osv-scanner-plugin/tree/main)

# Gradle OSV-Scanner Plugin

The Gradle OSV Scanner Plugin is a convenient integration of the [OSV Scanner](http://osv.dev), a standalone command-line tool for scanning open source dependencies for known vulnerabilities. This plugin simplifies the process of running vulnerability scans on your project's dependencies directly from your Gradle build scripts.

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

## Tasks

|               Task               |                                        Description                                        |         Osv Scanner Flag          |
|----------------------------------|-------------------------------------------------------------------------------------------|-----------------------------------|
| `osvInstall`                     | Installs appropriate version of osv-scanner based on the `os` and `arch` settings         |                                   |
| `osvInstallAll`                  | Installs all versions of osv-scanner allowing commiting of the binaries to the repository |                                   |
| `osvScan`                        | Scans the repository                                                                      | `--recursive`                     |
| `osvLockAndScan`                 | Creates the lockfiles, runs the scan as per `osvScan` and then deletes the lockfiles.     | `--recursive`                     |
| `osvSbom`                        | Runs a scan on an SBOM file as specified in the `sbom` configuration                      | `--sbom`                          |
| `osvLockfiles`                   | Runs a scan on the lockfiles specified in the `lockfiles` configuration                   | `--lockfile`                      |
| `osvExperimentalLicencesSummary` | Runs the licence summary                                                                  | `--experimental-licences-summary` |
| `osvExperimentalLicences`        | Runs the licence check with the list of licences defined in the `licence` setting         | `--experimental-licences`         |
| `writeLockfiles`                 | Generates the lockfiles                                                                   |                                   |
| `deleteLockfiles`                | Deletes all lockfiles in the project tree                                                 |                                   |

### Installation of osv-scanner

You can use the `osvInstall` task to download and install osv-scanner. By default, this will install the latest version of osv-scanner in the `build/osv-scanner` folder.

```bash
./gradlew osvInstall
```

Installation of a binary is within the `.osv-scanner` directory. The binaries are cached within a `.cache` directory within the
`.osv-scanner` directory. You should ensure that the `.osv-scanner/.cache` folder is in your `.gitignore` file. Optionally if you would rather dynamically install the `osv-scanner` binary then also add the `.osv-scanner` folder.

### Running a scan

You can use the `osvScan` task to initiate a vulnerability scan. By default, the scan will analyze all dependencies declared in your project, however note that for gradle projects you need to create lock files.
See [Locking dependency versions](https://docs.gradle.org/current/userguide/dependency_locking.html) in the Gradle documentation.

```bash
./gradlew osvScan
```

The plugin supports 3 failure modes on the results of the scan. Note that this is only supported of the `format` is `json`. To configure the failure mode see `failOn` and `failOnThreshold`

### Scanning an SBOM

You can use the `osvSbom` task to initiate a vulnerability scan on a specific SBOM file as specified by the `sbom` configuration item.

```bash
./gradlew osvSbom
```

### Scanning lockfiles

You can use the `osvLockfile` task to initiate a vulnerability scan on a specific lockfile or multiple lockfile files as specified by the `lockfiles` configuration item. The `lockfiles` is an array of lockfiles, or
a closure which will return an array of lockfiles. It is initialised with an array so you can just append
the lockfile onto the existing array.

```bash
./gradlew osvLockfile
```

```
osvScanner {
    lockfiles = ["/myproject/gradle.lockfile"]
}
```

```
osvScanner {
    lockfiles += "/myproject/gradle.lockfile"
}
```

```
osvScanner {
    lockfiles = {
        ["/myproject/gradle.lockfile"]
    }
}
```

### Experimental licences summary

Runs `osv-scanner` with the experimental licence flag `--experimental-licenses-summary`. The scan will write out a report in the `build/osv-scanner/` directory called `osv-scanner-exp-lic-sum` with the appropriate extension for the format.

```bash
./gradlew osvExperimentalLicencesSummary
```

### Experimental licences checking

Runs `osv-scanner` with the experimental licence flag `--experimental-licenses`. If you are using this flag then you must set
valid [SPDX](https://spdx.org/licenses/) licence identifiers as a comma-separated string with the `licences` value of the configuration. The scan will write out a report in the `build/osv-scanner/` directory called `osv-scanner-exp-lic` with the appropriate extension for the format.

```bash
./gradlew osvExperimentalLicences
```

```
osvScanner {
    licences "BSD-3-Clause,Apache-2.0,MIT"
}
```

### Configuration

The plugin supports a limited number of configurable settings that affect it's behaviour.

|       name        |                         values                         |                                                                          description                                                                          |
|-------------------|--------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `version`         | default: `latest`                                      | The version of osv-scanner to use, defaults to `latest` if not specified                                                                                      |
| `repository`      | default: `"google/osv-scanner"`                        | The GitHub repository to use to download the binary                                                                                                           |
| `os`              | `windows`, `linux`, `darwin`                           | The operating system for the binary, `darwin` represents Apple Macs. Defaults to detecting the os from the environment                                        |
| `arch`            | `amd64`, `arm64`                                       | The CPU architecture for the binary. Defaults to detecting the CPU architecture from the environment                                                          |
| `mode`            | `recursive`                                            | Currently this is the only mode supported                                                                                                                     |
| `format`          | `table`, `json`, `markdown`, `sarif`, `gh-annotations` | The format for the ouput report, defaults to `json`                                                                                                           |
| `flags`           | string `""`                                            | Any flags to pass through to osv-scanner                                                                                                                      |
| `binary`          | string `""`                                            | Optional specify the location of a pre-installed `osv-scanner` binary. If specified this will be used instead of a downloaded version using `osvInstall`      |
| `licences`        | string `""`                                            | Comma-separated list of valid [SPDX](https://spdx.org/licenses/) licence identifiers                                                                          |
| `sbom`            | string `""`                                            | Path to the SBOM file to scan                                                                                                                                 |
| `lockfiles`       | array []                                               | An array of paths to the lockfiles to scan, or a closure that resolves to an array                                                                            |
| `failOn`          | one of `exit` `count` or `score` default: `exit`       | `exit` faios on a non zero exit value from `osv-scanner`, `count` fails on the number of vulnerabilities found, `score` fails on the maximum cvss score found |
| `failOnThreshold` | decimal number, default `0`                            | The threshold number to apply for the `failOn` if `count` or `score` is specified                                                                             |

The following is an example configuration overriding the default version, format, passing through a flag and overriding the installation location of osv-scanner.

```
osvScanner {
    version = "v1.6.1"
    format = "json"
    flags = "--no-ignore"
    binary = "/tmp/osv-scanner"
    failOn = "score"
    failOnThreshold = 7.0
}
```

## License

This project is licensed under the [Apache 2.0 License](https://spdx.org/licenses/Apache-2.0.html).
