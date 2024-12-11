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

public class OSVScannerWriteLockfilesTask extends DefaultTask {

    public static final String NAME = "writeLockfiles"

    private Project project
    private ExecOperations execOps

    @Inject
    public OSVScannerWriteLockfilesTask(Project project, ExecOperations execOps) {
        this.project = project
        this.execOps = execOps
    }

    static register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        def taskContainer = project.getTasks()

        taskContainer.create([name: NAME,
            type: OSVScannerWriteLockfilesTask,
            dependsOn: [],
            group: OSVScannerPlugin.GROUP,
            description: 'Creates gradle lockfiles'])
    }

    @TaskAction
    def runTask() {
        OSVScannerWriteLockfilesTask.run(this.execOps)
    }

    static def run = { execOps ->
        execOps.exec(
            {
                commandLine = ["gradle", "dependencies", "--write-locks"]
            }
        )
    }

}
