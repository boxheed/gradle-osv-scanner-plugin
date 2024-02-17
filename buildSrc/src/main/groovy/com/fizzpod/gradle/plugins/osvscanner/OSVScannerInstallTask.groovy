package com.fizzpod.gradle.plugins.osvscanner

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import groovy.json.*
import javax.inject.Inject
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.io.FileUtils
import org.kohsuke.github.*

public class OSVScannerInstallTask extends DefaultTask {

    public static final String NAME = "osvInstall"

    public static final String WINDOWS = "windows"
    public static final String LINUX = "linux"
    public static final String MAC = "darwin"
    
    public static final String AMD64 = "amd64"
    public static final String ARM64 = "arm64"

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
            description: 'Creates a release on Github and uploads artefacts'])
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
        downloadScanner(context);
    }

    def getOs(def context) {
        def os = null
        if(context.extension.os != null) {
            os = context.extension.os
        } else  if(SystemUtils.IS_OS_WINDOWS) {
            os = WINDOWS
        } else if(SystemUtils.IS_OS_MAC) {
            os = MAC
        } else if(SystemUtils.IS_OS_LINUX) {
            os = LINUX
        }
        if(os == null) {
            throw new RuntimeException("Unsupported operating system for os-scanner: " + SystemUtils.OS_NAME)
        }
        context.logger.info("OS resolved to {}", os);
        return os;
    }

    def getArch(def context) {
        def systemArch = SystemUtils.OS_ARCH
        //Assume ARM
        def arch = ARM64
        if(context.extension.arch != null) {
            arch = context.extension.arch
        } else 
        if(systemArch.equalsIgnoreCase("x86_64") || systemArch.equalsIgnoreCase("amd64")) {
            return AMD64
        } 
        context.logger.info("Architecture resolved to {}", arch)
        return ARM64
    }
        
    def getRelease(def context) {
        def extension = context.extension
        GitHub github = GitHub.connectAnonymously();
        GHRepository osvScannerRepository = github.getRepository(extension.repository);
        GHRelease osvRelease = osvScannerRepository.getLatestRelease();
        //match against requeired release
        if(!"latest".equalsIgnoreCase(extension.version)) {
            Iterable<GHRelease> osvReleases = osvScannerRepository.listReleases();
            osvReleases.forEach(release -> {
                if(extension.version.equalsIgnoreCase(release.getName())) {
                    osvRelease = release
                }
            });
        }
        context.logger.info("osv-scanner version resolved to {}", osvRelease.getName())
        return osvRelease
    }

    def getAsset(def context) {
        def release = context.release
        def os = context.os
        def arch = context.arch
        //Find the appropriate binary asset
        Iterable<GHAsset> assets = release.listAssets();
        //find the appropriate asset
        GHAsset osvAsset = null;
        assets.forEach( asset -> {
            String assetName = asset.getName()
            if(assetName.contains(os) && assetName.contains(arch)) {
                osvAsset = asset;
            }
        })
        if(osvAsset == null) {
            throw new RuntimeException("Unable to find asset for operating system " + os + " and architecture " + arch)
        }
        return osvAsset
    }
    
    def downloadScanner(def context) {
        def project = context.project
        def extension = context.extension
        def buildDir = project.buildDir
        def asset = context.asset
        def url = asset.getBrowserDownloadUrl()
        context.logger.info("osv-scanner url resolved to {}", url)
        def installFolder = new File(buildDir, extension.location)
        def osvFile = new File(installFolder, OSVScannerPlugin.EXE_NAME)
        if(context.os.equals(WINDOWS)) {
            osvFile = new File(installFolder, OSVScannerPlugin.EXE_NAME + ".exe")
        }
        context.logger.info("Writing osv-scanner to  {}", osvFile)
        FileUtils.copyURLToFile(new URL(url), osvFile, 120000, 120000)
        osvFile.setExecutable(true)
    }

}