/* (C) 2024 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import org.gradle.api.Plugin
import org.gradle.api.Project

public class OSVScannerPlugin implements Plugin<Project> {

	public static final String NAME = "osvScanner"
	public static final String GROUP = "OSV Scanner"
	public static final String EXE_NAME = "osv-scanner"

	void apply(Project project) {
		project.extensions.create(NAME, OSVScannerPluginExtension)
		OSVScannerInstallTask.register(project)
		OSVScannerInstallAllTask.register(project)
		OSVScannerScanTask.register(project)
		OSVScannerLicencesTask.register(project)
		OSVScannerLicencesSummaryTask.register(project)
		OSVScannerSbomTask.register(project)
		OSVScannerLockfileTask.register(project)
		OSVScannerWriteLockfilesTask.register(project)
		OSVScannerDeleteLockfilesTask.register(project)
		OSVScannerLockAndScanTask.register(project)
	}
}
