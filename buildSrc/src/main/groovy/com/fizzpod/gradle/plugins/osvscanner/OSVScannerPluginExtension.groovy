/* (C) 2024-2026 */
/* SPDX-License-Identifier: Apache-2.0 */
package com.fizzpod.gradle.plugins.osvscanner

import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

public abstract class OSVScannerPluginExtension {

    abstract Property<String> getVersion()
    abstract Property<String> getLocation()
    abstract Property<String> getRepository()
    abstract Property<String> getOs()
    abstract Property<String> getArch()
    abstract Property<String> getMode()
    abstract Property<String> getFormat()
    abstract Property<String> getFlags()
    abstract Property<String> getBinary()
    abstract Property<String> getLicences()
    abstract Property<String> getSbom()
    abstract ListProperty<Object> getLockfiles()
    abstract Property<String> getFailOn()
    abstract Property<Double> getFailOnThreshold()

    @Inject
    public OSVScannerPluginExtension(ObjectFactory objects) {
        getVersion().convention("latest")
        getLocation().convention("osv-scanner")
        getRepository().convention("google/osv-scanner")
        getMode().convention("recursive")
        getFormat().convention("json")
        getFlags().convention("--no-ignore")
        getFailOn().convention("exit")
        getFailOnThreshold().convention(0.0d)
    }
}
