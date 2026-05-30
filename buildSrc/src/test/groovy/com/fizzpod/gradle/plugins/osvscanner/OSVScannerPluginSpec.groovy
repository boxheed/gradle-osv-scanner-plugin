/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import groovy.json.*
import org.apache.commons.io.FileUtils
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*
import spock.util.io.*

class OSVScannerPluginSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    @TempDir
    FileSystemFixture fsFixture

    
    def "initialise plugin"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile.test', 'gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile.test', 'settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()

        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)

        then: 
            project.getTasksByName(OSVScannerDeleteLockfilesTask.NAME, false) != null
            !project.getTasksByName(OSVScannerDeleteLockfilesTask.NAME, false).isEmpty()
            project.getExtensions().findByName(OSVScannerPlugin.NAME) != null
    }


    def "run osvScannerInstallTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile.test', 'gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile.test', 'settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()
            
        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            def resolveTask = project.getTasksByName(OSVScannerResolveVersionTask.NAME, false).iterator().next()
            resolveTask.runTask()
            println "Resolve Task Output: " + resolveTask.getResolvedVersionFile().get().asFile.absolutePath
            println "Exists? " + resolveTask.getResolvedVersionFile().get().asFile.exists()
            def task = project.getTasksByName(OSVScannerInstallTask.NAME, false).iterator().next()
            println "Install Task Input:  " + task.getResolvedVersionFile().get().asFile.absolutePath
            task.runTask()
        then: 
            !project.getTasksByName(OSVScannerInstallTask.NAME, false).isEmpty()
            def installDir = new File(root, ".osv-scanner")
            installDir.exists()
            // We can't guarantee which binary is downloaded without more complex mocking,
            // but at least one should be there or the cache directory.
            // This test is a bit flaky if it depends on external network.
            installDir.list().length > 0
    }

    def "run osvScannerInstallAllTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile.test', 'gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile.test', 'settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()
            
        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            project.getTasksByName(OSVScannerResolveVersionTask.NAME, false).iterator().next().runTask()
            def task = project.getTasksByName(OSVScannerInstallAllTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            !project.getTasksByName(OSVScannerInstallAllTask.NAME, false).isEmpty()
            def installDir = new File(root, ".osv-scanner")
            installDir.exists()
            new File(installDir, "osv-scanner_linux_amd64").exists()
            new File(installDir, "osv-scanner_linux_arm64").exists()
            new File(installDir, "osv-scanner_darwin_amd64").exists()
            new File(installDir, "osv-scanner_darwin_arm64").exists()
            new File(installDir, "osv-scanner_windows_amd64.exe").exists()
            new File(installDir, "osv-scanner_windows_arm64.exe").exists()
    }


    def "run osvScannerLicencesSummaryTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile.test', 'gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile.test', 'settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()

            // Mock OSV-Scanner binary
            def mockBinary = new File(root, "osv-scanner" + (System.getProperty("os.name").toLowerCase().contains("windows") ? ".bat" : ""))
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                mockBinary.text = "@echo off\r\necho {\"results\": []}"
            } else {
                mockBinary.text = "#!/bin/sh\necho '{\"results\": []}'"
                mockBinary.setExecutable(true)
            }

        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)

            // Configure extension to use mock binary
            def extension = project.extensions.getByType(OSVScannerPluginExtension)
            extension.getBinary().set(mockBinary.absolutePath)
           
            def task = project.getTasksByName(OSVScannerLicencesSummaryTask.NAME, false).iterator().next()
            task.getOsvScannerBinary().set(mockBinary) // explicitly set the binary for the task as well since we don't have the task evaluation graph here
            task.runTask()
        then: 
            !project.getTasksByName(OSVScannerLicencesSummaryTask.NAME, false).isEmpty()
            def buildDir = project.buildDir
            def reportFile = new File(buildDir, "osv-scanner/osv-scanner-exp-lic-sum.json")
            reportFile.exists()
            def json = new groovy.json.JsonSlurper().parse(reportFile)
            json.results != null
            json.results.size() == 0
    }

    @Ignore("Licence scanning is currently broken in OSV-Scanner, so this test is ignored until that is fixed")
    def "run osvScannerLicencesTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile.test', 'gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile.test', 'settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()

        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            project.getTasksByName(OSVScannerInstallTask.NAME, false).iterator().next().runTask()
            def task = project.getTasksByName(OSVScannerLicencesTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            //TODO proper assertion
            !project.getTasksByName(OSVScannerLicencesTask.NAME, false).isEmpty()
            def reportFile = new File(project.buildDir, "osv-scanner/osv-scanner-exp-lic.json")
            reportFile.exists()
            def json = new JsonSlurper().parseText(reportFile.text)
            json.license_summary != null
            !json.license_summary.isEmpty()
    }

    def "run OSVScannerScanTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile.test', 'gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile.test', 'settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()
        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            project.getTasksByName(OSVScannerResolveVersionTask.NAME, false).iterator().next().runTask()
            project.getTasksByName(OSVScannerInstallTask.NAME, false).iterator().next().runTask()
            def task = project.getTasksByName(OSVScannerScanTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            !project.getTasksByName(OSVScannerScanTask.NAME, false).isEmpty()
            def reportFile = new File(project.buildDir, 'osv-scanner/osv-scanner-scan.json')
            reportFile.exists()
            reportFile.length() > 0
            new groovy.json.JsonSlurper().parse(reportFile) != null
    }

    @Ignore
    def "run OSVScannerSbomTask"() {
        setup:
            
            Project project = ProjectBuilder.builder().withProjectDir(temporaryFolder.getRoot()).build()
            //copy the .osv-scanner directory
            FileUtils.copyDirectory(new File(FileUtils.current(), '.osv-scanner'), temporaryFolder.getRoot())
            //copy the .git directory
            FileUtils.copyDirectory(new File(FileUtils.current(), '.git'), temporaryFolder.getRoot())
            //copy the build.gradle
            FileUtils.copyFileToDirectory(new File(FileUtils.current(), 'build.gradle'), temporaryFolder.getRoot())
            //copy the settings.gradle
            FileUtils.copyFileToDirectory(new File(FileUtils.current(), 'settings.gradle'), temporaryFolder.getRoot())

        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            project.getTasksByName(OSVScannerInstallTask.NAME, false).iterator().next().runTask()
            def task = project.getTasksByName(OSVScannerSbomTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            //TODO proper assertion
            !project.getTasksByName(OSVScannerSbomTask.NAME, false).isEmpty()
    }


    @Ignore
    def "run OSVScannerLockAndScanTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile.test', 'gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile.test', 'settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()
            //copy the .osv-scanner directory
            //FileUtils.copyDirectory(new File(FileUtils.current(), '.osv-scanner'), temporaryFolder.getRoot())
            //copy the .git directory
            FileUtils.copyDirectory(new File(FileUtils.current(), '.git'), temporaryFolder.getRoot())
            //copy the build.gradle
            FileUtils.copyFileToDirectory(new File(FileUtils.current(), 'build.gradle'), temporaryFolder.getRoot())
            //copy the settings.gradle
            FileUtils.copyFileToDirectory(new File(FileUtils.current(), 'settings.gradle'), temporaryFolder.getRoot())

        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            def task = project.getTasksByName(OSVScannerLockAndScanTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            //TODO proper assertion
            !project.getTasksByName(OSVScannerLockAndScanTask.NAME, false).isEmpty()
    }
    
    @Ignore
    def "run OSVScannerLockfileTask"() {
        setup:
            Project project = ProjectBuilder.builder().withProjectDir(temporaryFolder.getRoot()).build()
            //copy the .osv-scanner directory
            FileUtils.copyDirectory(new File(FileUtils.current(), '.osv-scanner'), temporaryFolder.getRoot())
            //copy the .git directory
            FileUtils.copyDirectory(new File(FileUtils.current(), '.git'), temporaryFolder.getRoot())
            //copy the build.gradle
            FileUtils.copyFileToDirectory(new File(FileUtils.current(), 'build.gradle'), temporaryFolder.getRoot())
            //copy the settings.gradle
            FileUtils.copyFileToDirectory(new File(FileUtils.current(), 'settings.gradle'), temporaryFolder.getRoot())

        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            def task = project.getTasksByName(OSVScannerLockfileTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            //TODO proper assertion
            !project.getTasksByName(OSVScannerLockfileTask.NAME, false).isEmpty()
    }






    def "run OSVScannerWriteLockfilesTask"() {
        setup:
            fsFixture.create {
                file('settings.gradle').text = '''
                    rootProject.name = 'test'
                '''
                file('build.gradle').text = '''
                    plugins {
                        id 'java'
                    }
                    dependencyLocking {
                        lockAllConfigurations()
                    }
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        implementation 'junit:junit:4.13.2'
                    }
                '''
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()

            // Copy gradle wrapper files so the task can run gradlew in the sandbox directory
            def currentDir = FileUtils.current()
            FileUtils.copyFileToDirectory(new File(currentDir, "gradlew"), root)
            FileUtils.copyFileToDirectory(new File(currentDir, "gradlew.bat"), root)
            FileUtils.copyDirectory(new File(currentDir, "gradle"), new File(root, "gradle"))
            new File(root, "gradlew").setExecutable(true)

        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            def task = project.getTasksByName(OSVScannerWriteLockfilesTask.NAME, false).iterator().next()
            task.getProjectDir().set(root)
            task.getGradleExecutable().set(new File(root, System.getProperty("os.name").toLowerCase().contains("windows") ? "gradlew.bat" : "gradlew").absolutePath)
            task.runTask()
        then: 
            !project.getTasksByName(OSVScannerWriteLockfilesTask.NAME, false).isEmpty()
            new File(root, "gradle.lockfile").exists() || new File(root, "gradle/dependency-locks").exists()
    }

    def "osvSbom should fail when failOn is count and threshold is reached"() {
        setup:
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()

            // Mock OSV-Scanner binary that returns a vulnerability but exits with 0
            def mockBinary = new File(root, "osv-scanner" + (System.getProperty("os.name").toLowerCase().contains("windows") ? ".bat" : ""))
            def jsonOutput = '{"results": [{"packages": [{"vulnerabilities": [{}]}]}]}'
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                mockBinary.text = "@echo off\r\necho " + jsonOutput.replace('"', '^"')
            } else {
                mockBinary.text = "#!/bin/sh\necho '" + jsonOutput + "'"
                mockBinary.setExecutable(true)
            }

            def plugin = new OSVScannerPlugin()
            plugin.apply(project)

            // Configure extension
            def extension = project.extensions.getByType(OSVScannerPluginExtension)
            extension.getBinary().set(mockBinary.absolutePath)
            extension.getFailOn().set("count")
            extension.getFailOnThreshold().set(1.0d)
            extension.getSbom().set("some.sbom")
            extension.getFormat().set("json")

            def task = project.getTasksByName(OSVScannerSbomTask.NAME, false).iterator().next()
            task.getOsvScannerBinary().set(mockBinary)

        when:
            task.runTask()

        then:
            thrown(RuntimeException)
    }
}
