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
}
