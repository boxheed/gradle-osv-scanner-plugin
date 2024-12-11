/* (C) 2024 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import static com.fizzpod.gradle.plugins.osvscanner.OSVScannerHelper.*
import static com.fizzpod.gradle.plugins.osvscanner.OSVScannerRunnerTaskHelper.*

import groovy.json.*
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

public class OSVScannerLockAndScanTask extends DefaultTask {

    public static final String NAME = "osvLockAndScan"

    private Project project
    private ExecOperations execOps

    @Inject
    public OSVScannerLockAndScanTask(Project project, ExecOperations execOps) {
        this.project = project
        this.execOps = execOps
    }

    static register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        def taskContainer = project.getTasks()

        taskContainer.create([name: NAME,
            type: OSVScannerLockAndScanTask,
            dependsOn: [],
            group: OSVScannerPlugin.GROUP,
            description: 'Creates gradle lockfiles, runs the scan and then deletes the lockfiles'])
    }

    @TaskAction
    def runTask() {
        OSVScannerLockAndScanTask.run(this.project, this.execOps)
    }

    static def run = { project, execOps ->
        OSVScannerDeleteLockfilesTask.run(project)
        OSVScannerWriteLockfilesTask.run(execOps)
        OSVScannerScanTask.run(project)
        OSVScannerDeleteLockfilesTask.run(project)
    }

}
