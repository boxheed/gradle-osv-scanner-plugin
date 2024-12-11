/* (C) 2024 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import static com.fizzpod.gradle.plugins.osvscanner.OSVScannerHelper.*

import groovy.json.*
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

public class OSVScannerInstallAllTask extends OSVScannerInstallTask {

    public static final String NAME = "osvInstallAll"

    private Project project
    private def oses = [OSVScannerHelper.LINUX, OSVScannerHelper.MAC, OSVScannerHelper.WINDOWS] 
    private def arches = [OSVScannerHelper.AMD64, OSVScannerHelper.ARM64] 

    private def currentOs
    private def currentArch

    @Inject
    public OSVScannerInstallAllTask(Project project) {
        super(project)
        this.project = project
    }

    static register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        def taskContainer = project.getTasks()

        taskContainer.create([name: NAME,
            type: OSVScannerInstallAllTask,
            dependsOn: [],
            group: OSVScannerPlugin.GROUP,
            description: 'Downloads and installs all osv-scanner binaries'])
    }

    @TaskAction
    def runTask() {
        for(def os: oses) {
            currentOs = os
            for(def arch:arches) {
                currentArch = arch
                project.getLogger().lifecycle("Installating " + currentOs + ":" + currentArch)
                super.runTask()
            }
        }
    }
    def getAsset(def context) {
        context.os = currentOs
        context.arch = currentArch
        return super.getAsset(context)
    }
    
    def install(def context) {
        context.os = currentOs
        context.arch = currentArch
        super.install(context)
    }
    
    def download(def context) {
        context.os = currentOs
        context.arch = currentArch
        super.download(context)
    }
    

}
