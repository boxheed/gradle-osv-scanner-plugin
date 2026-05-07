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
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
public abstract class OSVScannerInstallAllTask extends DefaultTask {

    public static final String NAME = "osvInstallAll"

    private def osArches = [
        [OS.Family.LINUX.id, OS.Arch.AMD64.id],
        [OS.Family.LINUX.id, OS.Arch.ARM64.id],
        [OS.Family.MAC.id, OS.Arch.AMD64.id],
        [OS.Family.MAC.id, OS.Arch.ARM64.id],
        [OS.Family.WINDOWS.id, OS.Arch.AMD64.id],
        [OS.Family.WINDOWS.id, OS.Arch.ARM64.id]
    ]

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getResolvedVersionFile()

    @Input
    abstract Property<String> getOsvScannerRepository()

    @OutputDirectory
    abstract DirectoryProperty getOsvScannerLocation()

    @Inject
    public OSVScannerInstallAllTask(Project project) {
        def extension = project.extensions.getByType(OSVScannerPluginExtension)
        getOsvScannerRepository().convention(extension.getRepository())
        getOsvScannerLocation().convention(project.layout.projectDirectory.dir(".osv-scanner"))
    }

    static def register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        return project.tasks.register(NAME, OSVScannerInstallAllTask) {
            it.group = OSVScannerPlugin.GROUP
            it.description = 'Download and install all osv-scanner binaries'
        }
    }

    @TaskAction
    def runTask() {
        def version = getResolvedVersionFile().getAsFile().get().text.trim()
        for (def osArch : osArches) {
            def context = [:]
            context.version = version
            context.repo = getOsvScannerRepository().get()
            context.location = getOsvScannerLocation().getAsFile().get()

            context.os = OS.getOs(osArch[0])
            context.arch = OS.getArch(osArch[1])
            context.binary =
                    OSVScannerInstallation.binary(context.location, context.version, context.os, context.arch)

            Loggy.lifecycle("Installing {} : {} at {}", context.os, context.arch, context.binary)
            OSVScannerInstallTask.run(context)
        }
    }
}
