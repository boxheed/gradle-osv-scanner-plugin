package com.fizzpod.gradle.plugins.osvscanner

import spock.lang.Specification
import groovy.json.JsonBuilder

class PerformanceSpec extends Specification {

    def "benchmark failOnScore"() {
        setup:
        def context = [
            extension: [
                failOnThreshold: 10.0
            ],
            logger: [
                lifecycle: { msg -> },
                warn: { msg -> }
            ]
        ]

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
            OSVScannerRunnerTaskHelper.failOnScore(0, output, context)
        }
        long end = System.currentTimeMillis()
        println "Execution time for 10 iterations: ${end - start} ms"

        then:
        noExceptionThrown()
    }

    def "verify failOnCount logic"() {
        setup:
        def context = [
            extension: [
                failOnThreshold: 5 // Low threshold
            ],
            logger: [
                lifecycle: { msg -> },
                warn: { msg -> }
            ]
        ]
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
        OSVScannerRunnerTaskHelper.failOnCount(0, output, context)

        then:
        noExceptionThrown() // 2 < 5

        when:
        context.extension.failOnThreshold = 1
        OSVScannerRunnerTaskHelper.failOnCount(0, output, context)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("Vulnerabilities found; number found (2) exceeds threshold (1)")
    }

    def "verify failOnScore logic"() {
        setup:
        def context = [
            extension: [
                failOnThreshold: 9.0
            ],
            logger: [
                lifecycle: { msg -> println msg },
                warn: { msg -> println msg }
            ]
        ]
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
        OSVScannerRunnerTaskHelper.failOnScore(0, output, context)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("exceeds threshold (9.0)")

        when:
        context.extension.failOnThreshold = 10.0
        OSVScannerRunnerTaskHelper.failOnScore(0, output, context)

        then:
        noExceptionThrown()
    }
}
