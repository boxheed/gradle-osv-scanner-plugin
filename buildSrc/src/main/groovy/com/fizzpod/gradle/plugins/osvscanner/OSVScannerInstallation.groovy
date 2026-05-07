/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import groovy.json.*
import org.apache.commons.io.FileUtils
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory
import org.rauschig.jarchivelib.CompressionType

public class OSVScannerInstallation {

    static def install = { String repo, OS.Arch arch, OS.Family os, String version, File location ->
        def params = [
            arch: arch,
            os: os,
            location: location,
            version: version,
            repo: repo
        ]
        def context = [params: params]
        def result = Optional.ofNullable(context)
            .map(x -> OSVScannerInstallation.os(x))
            .map(x -> OSVScannerInstallation.arch(x))
            .map(x -> OSVScannerInstallation.artifact(x))
            .map(x -> OSVScannerInstallation.bin(x))
            .map(x -> OSVScannerInstallation.download(x))
            .map(x -> x.binary)
            .orElseThrow(() -> new RuntimeException("Unable to download osv-scanner"))
        return result
    }

    static def download = Loggy.wrap({ x ->
        if(!x.binary.exists()) {
            OSVScannerInstallation.downloadAndInstall(x.url, x.binary, x.os)
        } else {
            FileUtils.touch(x.binary)
        }
        return x.binary.exists()? x: null
    })

    static def downloadAndInstall = { url, binary, os ->
        def tmp = File.createTempFile("osv-scanner", ".download")
        tmp.deleteOnExit()
        FileUtils.copyURLToFile(new URL(url), tmp, 120000, 120000)
        if (url.endsWith(".gz")) {
            def archiver = ArchiverFactory.createArchiver(CompressionType.GZIP)
            archiver.extract(tmp, binary.getParentFile())
        } else if (url.endsWith(".zip")) {
            def archiver = ArchiverFactory.createArchiver(ArchiveFormat.ZIP)
            archiver.extract(tmp, binary.getParentFile())
            // Zip might contain osv-scanner.exe, rename to versioned name
            def exe = new File(binary.getParentFile(), "osv-scanner.exe")
            if (exe.exists()) {
                if (binary.exists()) {
                    binary.delete()
                }
                FileUtils.moveFile(exe, binary)
            }
        } else {
            if (binary.exists()) {
                binary.delete()
            }
            FileUtils.moveFile(tmp, binary)
        }
        binary.setExecutable(true)
        return binary
    }

    static def bin = Loggy.wrap({ x ->
        def location = x.params.location
        def version = x.version
        def os = x.os
        def arch = x.arch
        x.binary = OSVScannerInstallation.binary(location, version, os, arch)
        x.binary? x: null
    })

    static def binary = {location, version, os, arch ->
        def name = OSVScannerInstallation.getBinaryName(version, os, arch)
        return new File(location, name)
    }.memoize()


    static def getBinaryName = {version, os, arch ->
        def osId = os.id
        def archId = arch.id
        def extension = os == OS.Family.WINDOWS? ".exe": ""
        def name = "osv-scanner_${osId}_${archId}${extension}"
        return name
    }.memoize()

    static def artifact = Loggy.wrap({ x ->
        x = x + OSVScannerInstallation.resolveArtifact(x.params.repo, x.arch, x.os, x.params.version)
    })

    static def resolveArtifact = { String repo, OS.Arch arch, OS.Family os, String version ->
        def artifact = GitHubClient.resolve(repo, arch, os, version)
        return [url: artifact.url, version: artifact.version]
    }.memoize()

    static def os = Loggy.wrap({def x ->
        x.os = x.params.os
        x.os? x: null
    }.memoize())

    static def arch = Loggy.wrap({def x ->
        x.arch = x.params.arch
        x.arch? x: null
    }.memoize())

}
