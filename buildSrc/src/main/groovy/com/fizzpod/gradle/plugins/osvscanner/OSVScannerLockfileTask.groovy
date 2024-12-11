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
import org.kohsuke.github.*

public class OSVScannerLockfileTask extends DefaultTask {

    public static final String NAME = "osvLockfile"

    private Project project

    @Inject
    public OSVScannerLockfileTask(Project project) {
        this.project = project
    }

    static register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        def taskContainer = project.getTasks()

        taskContainer.create([name: NAME,
            type: OSVScannerLockfileTask,
            dependsOn: [],
            group: OSVScannerPlugin.GROUP,
            description: 'Runs osv-scanner with --lockfile on your project'])
    }

    @TaskAction
    def runTask() {
    
        def extension = project[OSVScannerPlugin.NAME]
        def context = [:]
        context.logger = project.getLogger()
        context.project = project
        context.extension = extension
        context.executable = getExecutable(context)
        context.mode = "lockfiles"
        context.lockfiles = getLockfiles(context)
        println(context.lockfiles)
        context.flags = getFlags(context)
        context.cmd = createCommand(context)
        context.failureMsg = "Vulnerability found."
        context.report = getReportFile(context)
        runCommand(context)
    }

    def getLockfiles(def context) {
        def lockfiles = context.extension.lockfiles
        if(( lockfiles instanceof Closure)) {
            return lockfiles.call()
        }
        return lockfiles
    }

    def createCommand(def context) {
        def extension = context.extension
        def mode = context.mode
        def lockfiles = context.lockfiles
        def commandParts = []
        commandParts.add(context.executable.getAbsolutePath())
        commandParts.add("--format")
        commandParts.add(extension.format)
        commandParts.add(context.flags)
        lockfiles.each( lockfile -> 
            commandParts.add("--lockfile=" + lockfile)
        )
        commandParts.add(context.project.projectDir)
        def command = commandParts.join(" ")
        return command
    }
}
