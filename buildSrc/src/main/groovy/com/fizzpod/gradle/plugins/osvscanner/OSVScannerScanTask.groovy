package com.fizzpod.gradle.plugins.osvscanner

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import groovy.json.*
import javax.inject.Inject
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.io.FileUtils
import org.kohsuke.github.*

public class OSVScannerScanTask extends DefaultTask {

    public static final String NAME = "osvScan"

    private Project project

    @Inject
    public OSVScannerScanTask(Project project) {
        this.project = project
    }

    static register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        def taskContainer = project.getTasks()

        taskContainer.create([name: NAME,
            type: OSVScannerScanTask,
            dependsOn: [],
            group: OSVScannerPlugin.GROUP,
            description: 'Runs osv-scanner on your project'])
    }

    @TaskAction
    def runTask() {
        def extension = project[OSVScannerPlugin.NAME]
        def context = [:]
        context.logger = project.getLogger()
        context.project = project
        context.extension = extension
        context.executable = getExecutable(context)
        context.flags = getFlags(context)
        context.cmd = createCommand(context)
        context.report = getReportFile(context)
        runScanner(context)
    }

    def getReportFile(def context) {
        def extension = context.extension
        def buildDir = project.buildDir
        def reportFolder = new File(buildDir, extension.location)
        def format = extension.format
        def suffix = format
        switch(format) {
            case 'json': suffix = "json"; break;
            case 'table': suffix = "txt"; break;
            case 'markdown': suffix = "md"; break;
            case 'sarif': suffix = "sarif"; break;
            default: suffix = "txt";
        }
        def reportFile = new File(reportFolder, OSVScannerPlugin.EXE_NAME + "." + suffix)
        return reportFile
    }

    def getExecutable(def context) {
        def extension = context.extension
        def buildDir = project.buildDir
        def installFolder = new File(buildDir, extension.location)
        def osvFile = new File(installFolder, OSVScannerPlugin.EXE_NAME)
        if(!osvFile.exists()) {
            osvFile = new File(installFolder, OSVScannerPlugin.EXE_NAME + ".exe")
        }
        if(!osvFile.exists()) {
            throw new RuntimeException("Cannot find osv-scanner binary")
        }
        context.logger.info("Using osv-scanner: {}", osvFile)
        return osvFile
    }

    def getFlags(def context) {
        return context.extension.flags
    }

    def createCommand(def context) {
        def extension = context.extension
        def commandParts = []
        commandParts.add(context.executable.getAbsolutePath())
        commandParts.add("--format")
        commandParts.add(extension.format)
        commandParts.add(context.flags)
        commandParts.add("--recursive")
        commandParts.add(context.project.projectDir)
        return commandParts.join(" ")
    }
        
    def runScanner(def context) {
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = context.cmd.execute()
        proc.waitForProcessOutput(sout, serr)
        proc.waitFor()
        def exitValue = proc.exitValue()
        def myFile = new File('mySuperFile.txt')
        context.logger.lifecycle(serr.toString())
        context.logger.lifecycle(sout.toString())
        context.report.write(sout.toString())
        context.logger.lifecycle("Output written to " + context.report)
        if(exitValue > 0 && exitValue < 127) {
            throw new RuntimeException("Vulnerabilities found")
        }
        if(exitValue >= 127) {
            throw new RuntimeException("An error has occured running osv-scanner. Exit: " + exitValue)
        }
        return
    }

    

}