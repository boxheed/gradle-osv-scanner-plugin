package com.fizzpod.gradle.plugins.osvscanner

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import groovy.json.*
import javax.inject.Inject
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.io.FileUtils
import org.kohsuke.github.*

import static com.fizzpod.gradle.plugins.osvscanner.OSVScannerHelper.*

public class OSVScannerRunnerTaskHelper extends DefaultTask {

    static def getReportFile(def context) {
        def extension = context.extension
        def mode = context.mode
        def buildDir = context.project.buildDir
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
        def reportFile = new File(reportFolder, OSVScannerPlugin.EXE_NAME +"-" + mode + "." + suffix)
        return reportFile
    }

    static def getExecutable(def context) {
        context.os = getOs(context)
        context.arch = getArch(context)
        def binary = getBinaryFromConfig(context)
        if(!binary.exists()) {
            throw new RuntimeException("Cannot find osv-scanner binary on path " + binary)
        }
        context.logger.info("Using osv-scanner: {}", binary)
        return binary
    }

    static def getFlags(def context) {
        return context.extension.flags
    }

    static def runCommand(def context) {
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = context.cmd.execute()
        proc.waitForProcessOutput(sout, serr)
        proc.waitFor()
        def exitValue = proc.exitValue()
        context.logger.lifecycle(serr.toString())
        context.logger.lifecycle(sout.toString())
        context.report.getParentFile().mkdirs()
        context.report.write(sout.toString())
        context.logger.lifecycle("Output written to " + context.report)
        //TODO this behaviour may need customising
        if(exitValue > 0 && exitValue < 127) {
            throw new RuntimeException(context.failureMsg + " Exit: " + exitValue)
        }
        if(exitValue >= 127) {
            throw new RuntimeException("An error has occured running osv-scanner. Exit: " + exitValue)
        }
        return
    }

}