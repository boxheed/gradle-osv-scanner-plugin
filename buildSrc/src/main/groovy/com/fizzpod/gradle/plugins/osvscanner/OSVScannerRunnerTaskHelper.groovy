/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream
import java.io.File
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import us.springett.cvss.Cvss

public class OSVScannerRunnerTaskHelper {

    static File getReportFile(File buildDir, String location, String mode, String format) {
        def reportFolder = new File(buildDir, location)
        def suffix = format
        switch(format) {
            case 'json': suffix = "json"; break
            case 'table': suffix = "txt"; break
            case 'markdown': suffix = "md"; break
            case 'sarif': suffix = "sarif"; break
            default: suffix = "txt"
        }
        return new File(reportFolder, "osv-scanner-" + mode + "." + suffix)
    }

    static void runCommand(ExecOperations execOps, List<String> commandList, File reportFile, Logger logger, String failOn, double failOnThreshold, String failureMsg) {
        def stdout = new ByteArrayOutputStream()
        def stderr = new ByteArrayOutputStream()
        
        def result = execOps.exec {
            it.commandLine commandList
            it.standardOutput = stdout
            it.errorOutput = stderr
            it.ignoreExitValue = true
        }
        
        def exitValue = result.exitValue
        def soutStr = stdout.toString()
        def serrStr = stderr.toString()
        
        logger.lifecycle(serrStr)
        logger.lifecycle(soutStr)
        
        reportFile.getParentFile().mkdirs()
        reportFile.text = soutStr
        logger.lifecycle("Output written to " + reportFile)
        
        if(exitValue >= 127) {
            throw new RuntimeException("An error has occured running osv-scanner. Exit: " + exitValue)
        }
        
        handleFailure(exitValue, soutStr, logger, failOn, failOnThreshold, failureMsg)
    }

    static void handleFailure(int exitValue, String output, Logger logger, String failOn, double failOnThreshold, String failureMsg) {
        switch(failOn) {
            case "count": failOnCount(output, failOnThreshold); break
            case "score": failOnScore(output, logger, failOnThreshold); break
            default: 
                if(exitValue > 0 && exitValue < 127) {
                    throw new RuntimeException(failureMsg + " Exit: " + exitValue)
                }
                break
        }
    }

    static void failOnCount(String output, double threshold) {
        def json = new JsonSlurper().parseText(output)
        def vulnCount = 0
        json.results?.each { result ->
            result.packages?.each { pkg ->
                pkg.vulnerabilities?.each { vuln ->
                    vulnCount++
                }
            }
        }
        if(vulnCount >= threshold) {
            throw new RuntimeException("Vulnerabilities found; number found ($vulnCount) exceeds threshold ($threshold).")
        }
    }

    static void failOnScore(String output, Logger logger, double threshold) {
        def json = new JsonSlurper().parseText(output)
        def cvssScore = 0.0
        json.results?.each { result ->
            result.packages?.each { pkg ->
                pkg.vulnerabilities?.each { vuln ->
                    vuln.severity?.each { sev ->
                        def score = Cvss.fromVector(sev.score).calculateScore().getBaseScore()
                        if (score > cvssScore) {
                            cvssScore = score
                        }
                    }
                }
            }
        }

        if(cvssScore != 0) {
            logger.lifecycle("Vulnerabilities found; max score ($cvssScore)")
        } else {
            logger.lifecycle("No vulnerabilities found")
        }
        if(cvssScore > threshold) {
            throw new RuntimeException("Vulnerabilities found; max score ($cvssScore) exceeds threshold ($threshold).")
        }
    }
}
