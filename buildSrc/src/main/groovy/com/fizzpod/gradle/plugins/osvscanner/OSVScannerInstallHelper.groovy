package com.fizzpod.gradle.plugins.osvscanner

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import groovy.json.*
import javax.inject.Inject
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.io.FileUtils
import org.kohsuke.github.*

public class OSVScannerHelper {

    public static final String WINDOWS = "windows"
    public static final String LINUX = "linux"
    public static final String MAC = "darwin"
    
    public static final String AMD64 = "amd64"
    public static final String ARM64 = "arm64"

    public static final String OSV_INSTALL_DIR = ".osv-scanner"


    static def getInstallRoot(def context) {
        def root = context.project.rootDir
        return new File(root, OSV_INSTALL_DIR)
    }

    static def getBinaryName(def context) {
        def name = "osv-scanner_" + getOs(context) + "_" + getArch(context)
        if(WINDOWS.equals(getOs(context))) {
            name = name + ".exe"
        }
        return name
    }
    
    static def getBinaryFile(def context) {
        return new File(getInstallRoot(context), getBinaryName(context))
    }

    static def getBinaryFromConfig(def context) {
        def binary = context.extension.binary
        if(binary != null && !"".equals(binary.trim())) {
            return new File(binary)
        }
        return getBinaryFile(context)
    }

    static def getOs(def context) {
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

    static def getArch(def context) {
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
    

}