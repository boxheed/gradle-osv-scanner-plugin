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
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations

@UntrackedTask(because="Generates reports")
public abstract class OSVScannerScanTask extends DefaultTask {

    public static final String NAME = "osvScan"

    @Input
    abstract Property<String> getFormat()

    @Input
    abstract Property<String> getFlags()

    @Input
    abstract Property<String> getFailOn()

    @Input
    abstract Property<Double> getFailOnThreshold()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getOsvScannerBinary()

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getProjectDir()

    @OutputFile
    abstract RegularFileProperty getReportFile()

    @Inject
    protected abstract ExecOperations getExecOperations()

    static def register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        def extension = project.extensions.getByType(OSVScannerPluginExtension)
        return project.tasks.register(NAME, OSVScannerScanTask) {
            it.group = OSVScannerPlugin.GROUP
            it.description = 'Runs osv-scanner on your project'
            it.getFormat().set(extension.format)
            it.getFlags().set(extension.flags)
            it.getFailOn().set(extension.failOn)
            it.getFailOnThreshold().set(extension.failOnThreshold)
            it.getProjectDir().set(project.layout.projectDirectory)
            it.getReportFile().set(project.layout.buildDirectory.file(extension.location.map { loc ->
                String format = extension.format.get()
                loc + "/osv-scanner-scan." + (format == 'json' ? 'json' : 'txt')
            }))
        }
    }

    @TaskAction
    void runTask() {
        File projectDir = getProjectDir().get().asFile
        File executable = getOsvScannerBinary().get().asFile
        
        List<String> commandList = [
            executable.absolutePath,
            "--format", getFormat().get(),
            "--recursive"
        ]
        if (getFlags().isPresent() && !getFlags().get().isEmpty()) {
            commandList.addAll(getFlags().get().split(" "))
        }
        commandList.add(projectDir.absolutePath)

        OSVScannerRunnerTaskHelper.runCommand(
            getExecOperations(),
            commandList,
            getReportFile().get().asFile,
            logger,
            getFailOn().get(),
            getFailOnThreshold().get(),
            "Vulnerabilities found."
        )
    }

}
