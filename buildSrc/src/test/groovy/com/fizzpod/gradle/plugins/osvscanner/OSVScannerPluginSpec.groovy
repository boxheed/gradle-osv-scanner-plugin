/* (C) 2024-2025 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

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
                copyFromClasspath('/gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile')
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
                copyFromClasspath('/gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()
            
        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            def task = project.getTasksByName(OSVScannerInstallTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            //TODO proper assertion
            !project.getTasksByName(OSVScannerInstallTask.NAME, false).isEmpty()
    }
    def "run osvScannerInstallAllTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()
            
        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            def task = project.getTasksByName(OSVScannerInstallAllTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            //TODO proper assertion
            !project.getTasksByName(OSVScannerInstallAllTask.NAME, false).isEmpty()
    }


    def "run osvScannerLicencesSummaryTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()

        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            project.getTasksByName(OSVScannerInstallTask.NAME, false).iterator().next().runTask()
           
            def task = project.getTasksByName(OSVScannerLicencesSummaryTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            //TODO proper assertion
            !project.getTasksByName(OSVScannerLicencesSummaryTask.NAME, false).isEmpty()
    }

    def "run osvScannerLicencesTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile')
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
    }

        def "run OSVScannerScanTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile')
            }
            def root = fsFixture.getCurrentPath().toFile()
            Project project = ProjectBuilder.builder().withProjectDir(root).build()
        when:
            def plugin = new OSVScannerPlugin()
            plugin.apply(project)
            project.getTasksByName(OSVScannerInstallTask.NAME, false).iterator().next().runTask()
            def task = project.getTasksByName(OSVScannerScanTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            //TODO proper assertion
            !project.getTasksByName(OSVScannerScanTask.NAME, false).isEmpty()
    }
/*
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


    def "run OSVScannerLockAndScanTask"() {
        setup:
            fsFixture.create {
                copyFromClasspath('/gradle.lockfile')
                copyFromClasspath('/settings-gradle.lockfile')
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
            def task = project.getTasksByName(OSVScannerWriteLockfilesTask.NAME, false).iterator().next()
            task.runTask()
        then: 
            //TODO proper assertion
            !project.getTasksByName(OSVScannerWriteLockfilesTask.NAME, false).isEmpty()
    }
    */
}
