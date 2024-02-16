package com.fizzpod.gradle.plugins.osvscanner

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import groovy.json.*
import javax.inject.Inject

public class OSVScannerDownloadTask extends DefaultTask {

    public static final NAME = "downloadOsvScanner"

    private Project project

    @Inject
    public OSVScannerDownloadTask(Project project) {
        this.project = project
    }

    static register(Project project) {
        project.getLogger().debug("Registering task {}", NAME)
        def taskContainer = project.getTasks()

        taskContainer.create([name: NAME,
            type: OSVScannerDownloadTask,
            dependsOn: [],
            group: OSVScannerPlugin.GROUP,
            description: 'Creates a release on Github and uploads artefacts'])
    }

        
    @TaskAction
    def runTask() {
        def extension = project[OSVScannerPlugin.NAME]
        def context = [:]
        context.project = project
        context.extension = extension
        
    }

}