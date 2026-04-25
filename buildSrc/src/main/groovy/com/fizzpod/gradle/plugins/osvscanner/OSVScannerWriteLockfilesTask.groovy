/* (C) 2024-2026 */
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

@org.gradle.api.tasks.UntrackedTask(because="Generates reports")
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
        OSVScannerWriteLockfilesTask.run(this.project, this.execOps)
    }

    static def run = { project, execOps ->
        if (project.gradle.startParameter.writeDependencyLocks) {
            project.logger.info("Gradle is already configured to write lockfiles, resolving dependencies to trigger lockfile generation")
            project.configurations.matching {
                it.canBeResolved
            }.each { it.resolve() }
        } else {
            project.logger.info("Gradle is not configured to write lockfiles, invoking Gradle with --write-locks flag")
            String executable = System.getProperty("org.gradle.appname", "gradle")
            if (executable == "gradlew") {
                String wrapperName = System.getProperty("os.name").toLowerCase().contains("windows") ? "gradlew.bat" : "gradlew"
                File wrapperScript = new File(project.getRootDir(), wrapperName)

                if (wrapperScript.exists()) {
                    project.logger.info("Gradle was invoked with wrapper: ${wrapperScript.absolutePath}")
                    executable = wrapperScript.getAbsolutePath()
                }
            }
            execOps.exec(
                {
                    commandLine = [executable, "dependencies", "--write-locks", NAME]
                }
            )
            return
        }

    }

}
