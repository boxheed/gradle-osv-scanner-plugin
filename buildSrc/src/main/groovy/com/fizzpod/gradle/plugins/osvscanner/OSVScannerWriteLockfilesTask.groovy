/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations

@UntrackedTask(because="Generates lockfiles")
public abstract class OSVScannerWriteLockfilesTask extends DefaultTask {

    public static final String NAME = "writeLockfiles"

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getProjectDir()

    @Input
    abstract Property<String> getGradleExecutable()

    @Inject
    protected abstract ExecOperations getExecOperations()

    static def register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        return project.tasks.register(NAME, OSVScannerWriteLockfilesTask) {
            it.group = OSVScannerPlugin.GROUP
            it.description = 'Creates gradle lockfiles'
            it.getProjectDir().set(project.layout.projectDirectory)
            it.getGradleExecutable().set(findGradleExecutable(project))
        }
    }

    @TaskAction
    void runTask() {
        run(getExecOperations(), getGradleExecutable().get(), getProjectDir().get().asFile)
    }

    static void run(ExecOperations execOps, String executable, File projectDir) {
        execOps.exec {
            it.workingDir projectDir
            it.commandLine = [executable, "dependencies", "--write-locks"]
            it.ignoreExitValue = false
        }
    }

    private static String findGradleExecutable(Project project) {
        String executable = System.getProperty("org.gradle.appname", "gradle")
        if (executable == "gradlew") {
            String wrapperName = System.getProperty("os.name").toLowerCase().contains("windows") ? "gradlew.bat" : "gradlew"
            File wrapperScript = new File(project.rootDir, wrapperName)
            if (wrapperScript.exists()) {
                executable = wrapperScript.absolutePath
            }
        }
        return executable
    }

}
