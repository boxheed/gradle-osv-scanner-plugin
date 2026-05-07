/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

public class OSVScannerPlugin implements Plugin<Project> {

	public static final String NAME = "osvScanner"
	public static final String GROUP = "OSV Scanner"
	public static final String EXE_NAME = "osv-scanner"

	void apply(Project project) {
		def extension = project.extensions.create(NAME, OSVScannerPluginExtension)
		
		def resolveVersionTask = OSVScannerResolveVersionTask.register(project)
		def installTask = OSVScannerInstallTask.register(project)
		def installAllTask = OSVScannerInstallAllTask.register(project)
		
		installTask.configure { task ->
			task.getResolvedVersionFile().set(resolveVersionTask.flatMap { it.getResolvedVersionFile() })
		}
		
		installAllTask.configure { task ->
			task.getResolvedVersionFile().set(resolveVersionTask.flatMap { it.getResolvedVersionFile() })
		}
		
		Provider<RegularFile> binaryProvider = extension.getBinary().map { bin ->
			if (bin != null && !bin.trim().isEmpty()) {
				return project.layout.projectDirectory.file(bin)
			}
			return null
		}.orElse(installTask.flatMap { it.getOsvScannerBinary() })

		def scanTask = OSVScannerScanTask.register(project)
		def licencesTask = OSVScannerLicencesTask.register(project)
		def licencesSummaryTask = OSVScannerLicencesSummaryTask.register(project)
		def sbomTask = OSVScannerSbomTask.register(project)
		def lockfileTask = OSVScannerLockfileTask.register(project)
		def writeLockfilesTask = OSVScannerWriteLockfilesTask.register(project)
		def deleteLockfilesTask = OSVScannerDeleteLockfilesTask.register(project)
		def lockAndScanTask = OSVScannerLockAndScanTask.register(project)
		
		[scanTask, licencesTask, licencesSummaryTask, sbomTask, lockfileTask, lockAndScanTask].each { taskProvider ->
			taskProvider.configure { t ->
				t.getOsvScannerBinary().set(binaryProvider)
			}
		}
	}
}
