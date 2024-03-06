package com.fizzpod.gradle.plugins.osvscanner

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import groovy.json.*
import javax.inject.Inject
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.io.FileUtils
import org.kohsuke.github.*
import groovy.json.JsonSlurper
import com.jayway.jsonpath.*
import us.springett.cvss.*

import static com.fizzpod.gradle.plugins.osvscanner.OSVScannerHelper.*

public class OSVScannerRunnerTaskHelper {

    private JsonSlurper jsonSlurper = new JsonSlurper()

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
        if(exitValue >= 127) {
            throw new RuntimeException("An error has occured running osv-scanner. Exit: " + exitValue)
        }
        failOn(exitValue, sout.toString(), context)
        return
    }

    static def failOn(def exitValue, def output, def context) {
        def extension = context.extension
        if(!"json".equals(extension.format) && !"exit".equals(extension.failOn)) {
            context.logger.warn("Only json format is supported for failOn. Ignoring failOn: " + extension.failOn)
            extension.failOn = "exit"
        }
        switch(extension.failOn) {
            case "count": failOnCount(exitValue, output, context); break;
            case "score": failOnScore(exitValue, output, context); break;
            default: failOnExit(exitValue, output, context); break;
        }
    }

    static def failOnCount(def exitValue, def output, def context) {
        def vulns = JsonPath.parse(output).read('$.results[*].packages[*].vulnerabilities[*]');
        def vulnCount = vulns.size()
        def threshold = context.extension.failOnThreshold
        if(vulnCount >= threshold) {
            throw new RuntimeException("Vulnerabilities found; number found ($vulnCount) exceeds threshold ($threshold).");
        }
    }

    static def failOnScore(def exitValue, def output, def context) {
        def severities = JsonPath.parse(output).read('$.results[*].packages[*].vulnerabilities[*].severity[*]');
        def cvssScore = 0
        def threshold = context.extension.failOnThreshold
        severities.each { item -> 
            def score = Cvss.fromVector(item.score).calculateScore().getBaseScore();
            switch(item.type) {
                case "CVSS_V2": cvssScore = cvssScore < score? score: cvssScore; break;
                default: cvssScore = cvssScore < score? score: cvssScore; break;
            }
        }

        if(cvssScore != 0) {
            context.logger.lifecycle("Vulnerabilities found; max score ($cvssScore)")
        } else {
            context.logger.lifecycle("No vulnerabilities found")
        }
        if(cvssScore > threshold) {
            throw new RuntimeException("Vulnerabilities found; max score ($cvssScore) exceeds threshold ($threshold).");
        }
    }

    static def failOnExit(def exitValue, def output, def context) {
        if(exitValue > 0 && exitValue < 127) {
            throw new RuntimeException(context.failureMsg + " Exit: " + exitValue)
        }
    }

}