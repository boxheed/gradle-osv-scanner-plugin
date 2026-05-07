/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Contains internal TTL logic for version resolution")
public abstract class OSVScannerResolveVersionTask extends DefaultTask {

    public static final String NAME = "osvResolveVersion"

    @Input
    abstract Property<String> getOsvScannerVersion()

    @Input
    abstract Property<String> getOsvScannerRepository()

    @Input
    abstract Property<Long> getTtl()

    @OutputDirectory
    abstract DirectoryProperty getVersionLocation()

    @OutputFile
    abstract RegularFileProperty getResolvedVersionFile()

    @Inject
    public OSVScannerResolveVersionTask(Project project) {
        def extension = project.extensions.getByType(OSVScannerPluginExtension)
        getOsvScannerVersion().convention(extension.getVersion())
        getOsvScannerRepository().convention(extension.getRepository())
        getTtl().convention(86400000L) // 1 day
        getVersionLocation().convention(project.layout.projectDirectory.dir(".osv-scanner"))
        getResolvedVersionFile().convention(getVersionLocation().file("version.txt"))
    }

    static def register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        return project.tasks.register(NAME, OSVScannerResolveVersionTask) {
            it.group = OSVScannerPlugin.GROUP
            it.description = "Resolves the 'latest' osv-scanner version from GitHub with TTL caching"
        }
    }

    @TaskAction
    def runTask() {
        File versionFile = getResolvedVersionFile().get().asFile
        long ttl = getTtl().get()

        if (versionFile.exists() && (System.currentTimeMillis() - versionFile.lastModified() < ttl)) {
            logger.info(
                    "Resolved version file is within TTL, skipping GitHub query. Using version {}",
                    versionFile.text)
            return
        }

        String version = getOsvScannerVersion().get()
        if (version.equalsIgnoreCase("latest")) {
            logger.lifecycle("Querying GitHub for the latest osv-scanner version.")
            def artifact =
                    OSVScannerInstallation.resolveArtifact(
                            getOsvScannerRepository().get(),
                            OS.getArch(null),
                            OS.getOs(null),
                            "latest")
            version = artifact.version
        } else {
            logger.info("Using configured osv-scanner version: {}", version)
        }

        versionFile.parentFile.mkdirs()
        versionFile.text = version
    }
}
