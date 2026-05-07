/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
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
public abstract class OSVScannerLockfileTask extends DefaultTask {

    public static final String NAME = "osvLockfile"

    @Input
    abstract Property<String> getFormat()

    @Input
    abstract Property<String> getFlags()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getOsvScannerBinary()

    @Input
    abstract ListProperty<Object> getLockfiles()

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
        return project.tasks.register(NAME, OSVScannerLockfileTask) {
            it.group = OSVScannerPlugin.GROUP
            it.description = 'Runs osv-scanner with --lockfile on your project'
            it.getFormat().set(extension.format)
            it.getFlags().set(extension.flags)
            it.getLockfiles().set(extension.lockfiles)
            it.getProjectDir().set(project.layout.projectDirectory)
            it.getReportFile().set(project.layout.buildDirectory.file(extension.location.map { loc ->
                String format = extension.format.get()
                loc + "/osv-scanner-lockfiles." + (format == 'json' ? 'json' : 'txt')
            }))
        }
    }

    @TaskAction
    void runTask() {
        File executable = getOsvScannerBinary().get().asFile
        
        List<Object> rawLockfiles = getLockfiles().get()
        List<String> resolvedLockfiles = []
        rawLockfiles.each {
            if (it instanceof Closure) {
                resolvedLockfiles.addAll(it.call())
            } else {
                resolvedLockfiles.add(it.toString())
            }
        }

        List<String> commandList = [
            executable.absolutePath,
            "--format", getFormat().get()
        ]
        if (getFlags().isPresent() && !getFlags().get().isEmpty()) {
            commandList.addAll(getFlags().get().split(" "))
        }
        resolvedLockfiles.each { lockfile -> 
            commandList.add("--lockfile=" + lockfile)
        }
        commandList.add(getProjectDir().get().asFile.absolutePath)

        OSVScannerRunnerTaskHelper.runCommand(
            getExecOperations(),
            commandList,
            getReportFile().get().asFile,
            logger,
            "exit",
            0.0,
            "Vulnerability found."
        )
    }
}
