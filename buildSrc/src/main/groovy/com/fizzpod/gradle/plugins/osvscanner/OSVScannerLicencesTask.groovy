/* (C) 2024-2025 */
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
import org.kohsuke.github.*

public class OSVScannerLicencesTask extends DefaultTask {

    public static final String NAME = "osvLicences"

    private Project project

    @Inject
    public OSVScannerLicencesTask(Project project) {
        this.project = project
    }

    static register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        def taskContainer = project.getTasks()

        taskContainer.create([name: NAME,
            type: OSVScannerLicencesTask,
            dependsOn: [],
            group: OSVScannerPlugin.GROUP,
            description: 'Runs osv-scanner with --experimental-licences on your project'])
    }

    @TaskAction
    def runTask() {
        def extension = project[OSVScannerPlugin.NAME]
        def context = [:]
        context.version = OSVScannerInstallTask.getInstalledVersion(project)
        context.logger = project.getLogger()
        context.project = project
        context.extension = extension
        context.executable = getExecutable(context)
        context.mode = "exp-lic"
        context.flags = getFlags(context)
        context.cmd = createCommand(context)
        context.failureMsg = "Licence violations found."
        context.report = getReportFile(context)
        runCommand(context)
    }

    def createCommand(def context) {
        def extension = context.extension
        def mode = context.mode
        def commandParts = []
        commandParts.add(context.executable.getAbsolutePath())
        commandParts.add("--format")
        commandParts.add(extension.format)
        commandParts.add(context.flags)
        if ( context.version.startsWith("v1")) {
            commandParts.add("--experimental-licenses=" + extension.licences)
        } else {
            commandParts.add("-licenses=" + extension.licences)
        }
        commandParts.add(context.project.projectDir)
        def command = commandParts.join(" ")
        return command
    }
        

}
