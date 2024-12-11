/* (C) 2024 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

public class OSVScannerPluginExtension {
	def version = "latest"
    def location = "osv-scanner"
    def repository = "google/osv-scanner"
    def os = null
    def arch = null
    def mode = "recursive"
    def format = "json"
    def flags = ""
    def binary = ""
    def licences = ""
    def sbom = ""
    def lockfiles = []
    def failOn = "exit"
    def failOnThreshold = 0
}
