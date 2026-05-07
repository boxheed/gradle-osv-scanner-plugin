/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
public abstract class OSVScannerInstallTask extends DefaultTask {

    public static final String NAME = "osvInstall"

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getResolvedVersionFile()

    @Input
    abstract Property<String> getOsvScannerRepository()

    @OutputDirectory
    abstract DirectoryProperty getOsvScannerLocation()

    @OutputFile
    abstract RegularFileProperty getOsvScannerBinary()

    @Inject
    public OSVScannerInstallTask(Project project) {
        def extension = project.extensions.getByType(OSVScannerPluginExtension)
        getOsvScannerRepository().convention(extension.getRepository())
        getOsvScannerLocation().convention(project.layout.projectDirectory.dir(".osv-scanner"))

        getOsvScannerBinary()
                .set(
                        getResolvedVersionFile()
                                .zip(
                                        getOsvScannerLocation(),
                                        { versionFile, location ->
                                            def file = versionFile.asFile
                                            def version =
                                                    file.exists()
                                                            ? file.text.trim()
                                                            : extension.getVersion().get()
                                            def os = OS.getOs(null)
                                            def arch = OS.getArch(null)
                                            def name =
                                                    OSVScannerInstallation.getBinaryName(
                                                            version, os, arch)
                                            return location.file(name)
                                        }))
    }

    static def register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        return project.tasks.register(NAME, OSVScannerInstallTask) {
            it.group = OSVScannerPlugin.GROUP
            it.description = 'Downloads and installs osv-scanner'
        }
    }

    @TaskAction
    def runTask() {
        def context = [:]
        context.repo = getOsvScannerRepository().get()
        context.arch = OS.getArch(null)
        context.os = OS.getOs(null)
        def vFile = getResolvedVersionFile().getAsFile().get()
        println("Inside runTask, file path: " + vFile.absolutePath + ", exists? " + vFile.exists())
        context.version = vFile.text.trim()
        context.location = getOsvScannerLocation().getAsFile().get()
        context.binary = getOsvScannerBinary().get().asFile

        if (!context.binary.exists()) {
            OSVScannerInstallTask.run(context)
        }
    }

    static def getInstalledVersion(File installRoot) {
        File versionFile = new File(installRoot, "version.txt")
        return versionFile.exists() ? versionFile.text.trim() : "v2"
    }

    static def run = { context ->
        return java.util.Optional.ofNullable(context)
                .map(x -> OSVScannerInstallTask.download(x))
                .orElseThrow(() -> new RuntimeException("Unable to install osv-scanner"))
    }

    static def download = { context ->
        def artifact =
                OSVScannerInstallation.resolveArtifact(
                        context.repo, context.arch, context.os, context.version)
        OSVScannerInstallation.downloadAndInstall(artifact.url, context.binary, context.os)
        return context
    }
}
