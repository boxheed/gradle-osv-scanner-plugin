/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import groovy.json.JsonBuilder
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import spock.lang.Specification

class PerformanceSpec extends Specification {

    private static final Logger LOGGER = Logging.getLogger(PerformanceSpec)

    def "benchmark failOnScore"() {
        setup:
        def threshold = 10.0

        // Generate a large JSON
        def builder = new JsonBuilder()
        def largeData = [
            results: (1..100).collect {
                [
                    packages: (1..10).collect {
                        [
                            vulnerabilities: (1..5).collect {
                                [
                                    severity: (1..2).collect {
                                        [
                                            type: "CVSS_V3",
                                            score: "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        ]
        builder(largeData)
        def output = builder.toString()

        when:
        long start = System.currentTimeMillis()
        for(int i=0; i<10; i++) {
            OSVScannerRunnerTaskHelper.failOnScore(output, LOGGER, threshold)
        }
        long end = System.currentTimeMillis()
        println "Execution time for 10 iterations: ${end - start} ms"

        then:
        noExceptionThrown()
    }

    def "verify failOnCount logic"() {
        setup:
        def threshold = 5.0
        def builder = new JsonBuilder()
        // 2 vulns
        def data = [
            results: [
                [
                    packages: [
                        [
                            vulnerabilities: [
                                [id: "VULN-1"],
                                [id: "VULN-2"]
                            ]
                        ]
                    ]
                ]
            ]
        ]
        builder(data)
        def output = builder.toString()

        when:
        OSVScannerRunnerTaskHelper.failOnCount(output, threshold)

        then:
        noExceptionThrown() // 2 < 5

        when:
        threshold = 1.0
        OSVScannerRunnerTaskHelper.failOnCount(output, threshold)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("Vulnerabilities found; number found (2) exceeds threshold (1.0)")
    }

    def "verify failOnScore logic"() {
        setup:
        def threshold = 9.0
        def builder = new JsonBuilder()
        // Score High (CVSS 3.1 Base Score for this vector is 9.8)
        def data = [
            results: [
                [
                    packages: [
                        [
                            vulnerabilities: [
                                [
                                    severity: [
                                        [
                                            type: "CVSS_V3",
                                            score: "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ]
            ]
        ]
        builder(data)
        def output = builder.toString()

        when:
        OSVScannerRunnerTaskHelper.failOnScore(output, LOGGER, threshold)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("exceeds threshold (9.0)")

        when:
        threshold = 10.0
        OSVScannerRunnerTaskHelper.failOnScore(output, LOGGER, threshold)

        then:
        noExceptionThrown()
    }
}
