/*
 * Copyright (c) 2016. Fengguo Wei and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Detailed contributors are listed in the CONTRIBUTOR.md
 */

package org.argus.jawa.android.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * @author <a href="mailto:fgwei521@gmail.com">Fengguo Wei</a>
 */
class AndroidJawaPluginTest {
    Project project

    @Before
    public void setUp() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: androidPluginName()
    }

    public String androidPluginName() {
        "android"
    }

    @Test
    public void applyingBeforeAndroidPluginShouldThrowException() {
        project = ProjectBuilder.builder().build()
        try {
            project.apply plugin: "org.argus.android-jawa"
            Assert.fail("Should throw Exception")
        } catch (GradleException e) {
        }
    }

    @Test
    public void applyingAfterAndroidPluginShouldNeverThrowException() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: androidPluginName()
        project.apply plugin: "org.argus.android-jawa" // never throw Exception
    }

    def getPlugin() {
        project.apply plugin: "org.argus.android-jawa"
        project.plugins.findPlugin(AndroidJawaPlugin)
    }

    @Test
    public void addDefaultJawaMainSourceSetToAndroidPlugin() {
        def plugin = getPlugin()
        Assert.assertEquals([], plugin.sourceDirectorySetMap["main"].files.toList())
        def src1 = new File(project.file("."), ["src", "main", "jawa", "Src1.jawa"].join(File.separator))
        src1.parentFile.mkdirs()
        src1.withWriter { it.write("record `Src1` @kind interface @AccessFlag PUBLIC_ABSTRACT_INTERFACE {}") }
        Assert.assertEquals([src1], plugin.sourceDirectorySetMap["main"].files.toList())
        Assert.assertEquals([], plugin.sourceDirectorySetMap["androidTest"].files.toList())
    }

    @Test
    public void addCustomFlavorJawaSourceSetToAndroidPlugin() {
        project.android { productFlavors { customFlavor { } } }
        def plugin = getPlugin()
        Assert.assertEquals([], plugin.sourceDirectorySetMap["customFlavor"].files.toList())

        def src = new File(project.file("."), ["src", "customFlavor", "jawa", "Src.jawa"].join(File.separator))
        src.parentFile.mkdirs()
        src.withWriter { it.write("record `Src` @kind interface @AccessFlag PUBLIC_ABSTRACT_INTERFACE {}") }

        def testSrc = new File(project.file("."), ["src", "androidTestCustomFlavor", "jawa", "TestSrc.jawa"].join(File.separator))
        testSrc.parentFile.mkdirs()
        testSrc.withWriter { it.write("record `TestSrc` @kind interface @AccessFlag PUBLIC_ABSTRACT_INTERFACE {}") }

        Assert.assertEquals([src], plugin.sourceDirectorySetMap["customFlavor"].files.toList())
        Assert.assertEquals([testSrc], plugin.sourceDirectorySetMap["androidTestCustomFlavor"].files.toList())
    }

    @Test
    public void addDefaultJawaAndroidTestSourceSetToAndroidPlugin() {
        def plugin = getPlugin()
        Assert.assertEquals([], plugin.sourceDirectorySetMap["androidTest"].files.toList())
        def src1 = new File(project.file("."), ["src", "androidTest", "jawa", "Src1Test.jawa"].join(File.separator))
        src1.parentFile.mkdirs()
        src1.withWriter { it.write("record `Src1Test` @kind interface @AccessFlag PUBLIC_ABSTRACT_INTERFACE {}") }
        Assert.assertEquals([], plugin.sourceDirectorySetMap["main"].files.toList())
        Assert.assertEquals([src1], plugin.sourceDirectorySetMap["androidTest"].files.toList())
    }

    @Test
    public void addCustomJawaMainSourceSetToAndroidPlugin() {
        def plugin = getPlugin()
        def defaultSrc = new File(project.file("."), ["src", "main", "jawa", "Src1.jawa"].join(File.separator))
        def customSrc = new File(project.file("."), ["custom", "sourceSet", "Src2.jawa"].join(File.separator))
        defaultSrc.parentFile.mkdirs()
        defaultSrc.withWriter { it.write("record `Src1` @kind interface @AccessFlag PUBLIC_ABSTRACT_INTERFACE {}") }
        customSrc.parentFile.mkdirs()
        customSrc.withWriter { it.write("record `Src2` @kind interface @AccessFlag PUBLIC_ABSTRACT_INTERFACE {}") }
        project.android { sourceSets { main { jawa { srcDir "custom/sourceSet" } } } }
        Assert.assertEquals([customSrc, defaultSrc], plugin.sourceDirectorySetMap["main"].files.toList().sort())
        Assert.assertEquals([], plugin.sourceDirectorySetMap["androidTest"].files.toList().sort())
    }

    @Test
    public void addCustomJawaAndroidTestSourceSetToAndroidPlugin() {
        def plugin = getPlugin()
        def defaultSrc = new File(project.file("."), ["src", "androidTest", "jawa", "Src1.jawa"].join(File.separator))
        def customSrc = new File(project.file("."), ["custom", "sourceSet", "Src2.jawa"].join(File.separator))
        defaultSrc.parentFile.mkdirs()
        defaultSrc.withWriter { it.write("record `Src1` @kind interface @AccessFlag PUBLIC_ABSTRACT_INTERFACE {}") }
        customSrc.parentFile.mkdirs()
        customSrc.withWriter { it.write("record `Src2` @kind interface @AccessFlag PUBLIC_ABSTRACT_INTERFACE {}") }
        project.android { sourceSets { androidTest { jawa { srcDir "custom/sourceSet" } } } }
        Assert.assertEquals([], plugin.sourceDirectorySetMap["main"].files.toList().sort())
        Assert.assertEquals([customSrc, defaultSrc], plugin.sourceDirectorySetMap["androidTest"].files.toList().sort())
    }

    @Test
    public void updateCustomJawaMainSourceSetToAndroidPlugin() {
        def plugin = getPlugin()
        def customSrc = new File(project.file("."), ["custom", "sourceSet", "Src2.jawa"].join(File.separator))
        customSrc.parentFile.mkdirs()
        customSrc.withWriter { it.write("record `Src2` @kind interface @AccessFlag PUBLIC_ABSTRACT_INTERFACE {}") }
        project.android { sourceSets { main { jawa { srcDirs = ["custom/sourceSet"] } } } }
        Assert.assertEquals([customSrc], plugin.sourceDirectorySetMap["main"].files.toList().sort())
        Assert.assertEquals([], plugin.sourceDirectorySetMap["androidTest"].files.toList().sort())
    }

    @Test
    public void updateCustomJawaAndroidTestSourceSetToAndroidPlugin() {
        def plugin = getPlugin()
        def customSrc = new File(project.file("."), ["custom", "testSourceSet", "Src1Test.jawa"].join(File.separator))
        customSrc.parentFile.mkdirs()
        customSrc.withWriter { it.write("record `Src1Test` @kind interface @AccessFlag PUBLIC_ABSTRACT_INTERFACE {}") }
        project.android { sourceSets { androidTest { jawa { srcDirs = ["custom/testSourceSet"] } } } }
        Assert.assertEquals([], plugin.sourceDirectorySetMap["main"].files.toList().sort())
        Assert.assertEquals([customSrc], plugin.sourceDirectorySetMap["androidTest"].files.toList().sort())
    }

//    @Test
//    public void jawaVersionFromClasspath() {
//        def classpath = System.getProperty("java.class.path").split(File.pathSeparator).collect { new File(it) }
//        Assert.assertEquals("2.11.7", AndroidScalaPlugin.scalaVersionFromClasspath(classpath))
//    }
}
