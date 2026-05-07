/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import java.io.File
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.logging.Logger

public class OSVScannerHelper {

    public static final String WINDOWS = "windows"
    public static final String LINUX = "linux"
    public static final String MAC = "darwin"
    
    public static final String AMD64 = "amd64"
    public static final String ARM64 = "arm64"

    public static final String OSV_INSTALL_DIR = ".osv-scanner"

    static File getInstallRoot(File rootDir) {
        return new File(rootDir, OSV_INSTALL_DIR)
    }

    static String getBinaryName(String os, String arch) {
        def name = "osv-scanner_" + os + "_" + arch
        if(WINDOWS.equals(os)) {
            name = name + ".exe"
        }
        return name
    }
    
    static File getBinaryFile(File rootDir, String os, String arch) {
        return new File(getInstallRoot(rootDir), getBinaryName(os, arch))
    }

    static File getBinary(String configBinary, File rootDir, String os, String arch) {
        if(configBinary != null && !configBinary.trim().isEmpty()) {
            return new File(configBinary)
        }
        return getBinaryFile(rootDir, os, arch)
    }

    static String resolveOs(String configOs, Logger logger) {
        def os = null
        if(configOs != null && !configOs.trim().isEmpty()) {
            os = configOs
        } else if(SystemUtils.IS_OS_WINDOWS) {
            os = WINDOWS
        } else if(SystemUtils.IS_OS_MAC) {
            os = MAC
        } else if(SystemUtils.IS_OS_LINUX) {
            os = LINUX
        }
        if(os == null) {
            throw new RuntimeException("Unsupported operating system for os-scanner: " + SystemUtils.OS_NAME)
        }
        logger.info("OS resolved to {}", os)
        return os
    }

    static String resolveArch(String configArch, Logger logger) {
        if(configArch != null && !configArch.trim().isEmpty()) {
            return configArch
        }
        def systemArch = SystemUtils.OS_ARCH
        def arch = ARM64
        if(systemArch.equalsIgnoreCase("x86_64") || systemArch.equalsIgnoreCase("amd64")) {
            arch = AMD64
        } 
        logger.info("Architecture resolved to {}", arch)
        return arch
    }
}
