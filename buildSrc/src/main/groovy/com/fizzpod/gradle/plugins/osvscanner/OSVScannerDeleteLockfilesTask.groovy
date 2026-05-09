/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because="Deletes lockfiles")
public abstract class OSVScannerDeleteLockfilesTask extends DefaultTask {

    public static final String NAME = "deleteLockfiles"

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getProjectDir()

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations()

    static def register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        return project.tasks.register(NAME, OSVScannerDeleteLockfilesTask) {
            it.group = OSVScannerPlugin.GROUP
            it.description = 'Deletes gradle lockfiles'
            it.getProjectDir().set(project.layout.projectDirectory)
        }
    }

    @TaskAction
    void runTask() {
        def projectDir = getProjectDir().get()
        def tree = projectDir.getAsFileTree().matching { include("**/*.lockfile") }
        run(getFileSystemOperations(), tree)
    }

    static void run(FileSystemOperations fsOps, org.gradle.api.file.FileTree tree) {
        fsOps.delete {
            it.delete(tree)
        }
    }

}
