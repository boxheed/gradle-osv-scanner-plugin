/* (C) 2024 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import static com.fizzpod.gradle.plugins.osvscanner.OSVScannerHelper.*

import groovy.json.*
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.kohsuke.github.*

public class OSVScannerInstallTask extends DefaultTask {

    public static final String NAME = "osvInstall"

    public static final String WINDOWS = "windows"
    public static final String LINUX = "linux"
    public static final String MAC = "darwin"
    
    public static final String AMD64 = "amd64"
    public static final String ARM64 = "arm64"

    public static final String OSV_INSTALL_DIR = ".osv-scanner"

    private Project project

    @Inject
    public OSVScannerInstallTask(Project project) {
        this.project = project
    }

    static register(Project project) {
        project.getLogger().info("Registering task {}", NAME)
        def taskContainer = project.getTasks()

        taskContainer.create([name: NAME,
            type: OSVScannerInstallTask,
            dependsOn: [],
            group: OSVScannerPlugin.GROUP,
            description: 'Downloads and installs osv-scanner'])
    }

    @TaskAction
    def runTask() {
        def extension = project[OSVScannerPlugin.NAME]
        def context = [:]
        context.logger = project.getLogger()
        context.project = project
        context.extension = extension
        context.os = getOs(context)
        context.arch = getArch(context)
        context.release = getRelease(context)
        context.asset = getAsset(context)
        context.location = getCacheLocation(context)
        download(context)
        install(context)
    }

    def install(def context) {
        def version = context.release.getName()
        def installFolder = getInstallRoot(context)
        def osvFile = new File(installFolder, context.location.getName())
        def versionFile = new File(installFolder, 'osv-scanner.version')
        def contents = ""
        if(versionFile.exists()) {
            contents = versionFile.getText()
        }
        if(version.equalsIgnoreCase(contents) && osvFile.exists()) {
            return
        }
        FileUtils.copyFile(context.location, osvFile)
        osvFile.setExecutable(true)
        versionFile.write(context.release.getName())
    }
    def getInstallRoot(def context) {
        def root = context.project.rootDir
        return new File(root, OSV_INSTALL_DIR)
    }

//TODO Think about using a .cache directory and downloading binaries for all OS and ARCH

    def getCacheLocation(def context) {
        def root = context.project.rootDir
        def osvInstallRoot = new File(root, OSV_INSTALL_DIR)
        def release = context.release
        def version = release.getName()

        def osvInstallLocation = new File(osvInstallRoot, ".cache/" + version)
        def osvFileName = getBinaryName(context.os, context.arch)
        def osvFile = new File(osvInstallLocation, osvFileName)
        return osvFile
    }

    def getRelease(def context) {
        def extension = context.extension
        GitHub github = GitHub.connectAnonymously()
        GHRepository osvScannerRepository = github.getRepository(extension.repository)
        GHRelease osvRelease = osvScannerRepository.getLatestRelease()
        //match against requeired release
        if(!"latest".equalsIgnoreCase(extension.version)) {
            Iterable<GHRelease> osvReleases = osvScannerRepository.listReleases()
            osvReleases.forEach(release -> {
                if(extension.version.equalsIgnoreCase(release.getName())) {
                    osvRelease = release
                }
            })
        }
        context.logger.info("osv-scanner version resolved to {}", osvRelease.getName())
        return osvRelease
    }

    def getAsset(def context) {
        def release = context.release
        def os = context.os
        def arch = context.arch
        //Find the appropriate binary asset
        Iterable<GHAsset> assets = release.listAssets()
        //find the appropriate asset
        GHAsset osvAsset = null
        assets.forEach( asset -> {
            String assetName = asset.getName()
            if(assetName.contains(os) && assetName.contains(arch)) {
                osvAsset = asset
            }
        })
        if(osvAsset == null) {
            throw new RuntimeException("Unable to find asset for operating system " + os + " and architecture " + arch)
        }
        return osvAsset
    }
    
    def download(def context) {
        def project = context.project
        def extension = context.extension
        def buildDir = project.buildDir
        def asset = context.asset
        def url = asset.getBrowserDownloadUrl()
        context.logger.info("osv-scanner url resolved to {}", url)
        def osvFile = context.location
        context.logger.info("Writing osv-scanner to  {}", osvFile)
        if(!osvFile.exists()){
            FileUtils.copyURLToFile(new URL(url), osvFile, 120000, 120000)
            osvFile.setExecutable(true)
        }
    }

}
