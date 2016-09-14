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

import com.google.common.annotations.VisibleForTesting
import org.apache.commons.io.FileUtils
import org.argus.jawa.gradle.plugins.JawaJarFile
import org.argus.jawa.gradle.tasks.DefaultJawaSourceSet
import org.argus.jawa.gradle.tasks.JawaRuntime
import org.argus.jawa.gradle.tasks.compile.JawaCompile
import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySetFactory
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory

import javax.inject.Inject
import java.util.concurrent.atomic.AtomicReference

/**
 * AndroidJawaPlugin adds jawa language support to official gradle android plugin.
 *
 * @author <a href="mailto:fgwei521@gmail.com">Fengguo Wei</a>
 */
public class AndroidJawaPlugin implements Plugin<Project> {
    private final FileResolver fileResolver
    @VisibleForTesting
    final Map<String, SourceDirectorySet> sourceDirectorySetMap = new HashMap<>()
    private Project project
    private Object androidPlugin
    private Object androidExtension
    private boolean isLibrary
    private File workDir

    /**
     * Creates a new AndroidJawaPlugin with given file resolver.
     *
     * @param fileResolver the FileResolver
     */
    @Inject
    public AndroidJawaPlugin(FileResolver fileResolver) {
        this.fileResolver = fileResolver
    }

    /**
     * Registers the plugin to current project.
     *
     * @param project currnet project
     * @param androidExtension extension of Android Plugin
     */
    void apply(Project project, Object androidExtension) {
        this.project = project
        if (project.plugins.hasPlugin("android-library")) {
            isLibrary = true
            androidPlugin = project.plugins.findPlugin("android-library")
        } else {
            isLibrary = false
            androidPlugin = project.plugins.findPlugin("android")
        }
        this.androidExtension = androidExtension
        this.workDir = new File(project.buildDir, "android-jawa")
//        updateAndroidExtension()
        updateAndroidSourceSetsExtension()
        androidExtension.buildTypes.whenObjectAdded { updateAndroidSourceSetsExtension() }
        androidExtension.productFlavors.whenObjectAdded { updateAndroidSourceSetsExtension() }
        androidExtension.signingConfigs.whenObjectAdded { updateAndroidSourceSetsExtension() }

        project.afterEvaluate {
            updateAndroidSourceSetsExtension()
            androidExtension.sourceSets.each { it.java.srcDirs(it.jawa.srcDirs) }
            def allVariants = androidExtension.testVariants + (isLibrary ? androidExtension.libraryVariants : androidExtension.applicationVariants)
            allVariants.each { variant ->
                addAndroidJawaCompileTask(variant)
            }
        }

        project.tasks.findByName("preBuild").doLast {
            FileUtils.forceMkdir(workDir)
        }
    }

    /**
     * Registers the plugin to current project.
     *
     * @param project currnet project
     */
    public void apply(Project project) {
        if (!["com.android.application",
              "android",
              "com.android.library",
              "android-library",
              "com.android.model.application",
              "com.android.model.library"].any { project.plugins.findPlugin(it) }) {
            throw new ProjectConfigurationException("Please apply 'com.android.application' or 'com.android.library' plugin before applying 'android-jawa' plugin", null)
        }
        apply(project, project.extensions.getByName("android"))
    }

    /**
     * Returns directory for plugin's private working directory for argument
     *
     * @param variant the Variant
     * @return
     */
    File getVariantWorkDir(Object variant) {
        new File([workDir, "variant", variant.name].join(File.separator))
    }

    /**
     * Returns jawa version from jawa-compiler in given classpath.
     *
     * @param classpath the classpath contains jawa-compiler
     * @return jawa version
     */
    static String jawaVersionFromClasspath(Collection<File> classpath) {
//        def jawajar = JawaRuntime.findJawaJarFile(classpath)
//        return jawajar == null ? null: jawajar.version.toString()
        return "1.0.3"
    }

    /**
     * Updates AndroidPlugin's root extension to work with AndroidJawaPlugin.
     */
//    void updateAndroidExtension() {
//        androidExtension.metaClass.getJawa = { extension }
//        androidExtension.metaClass.jawa = { configureClosure ->
//            ConfigureUtil.configure(configureClosure, extension)
//            androidExtension
//        }
//    }

    /**
     * Updates AndroidPlugin's sourceSets extension to work with AndroidJawaPlugin.
     */
    void updateAndroidSourceSetsExtension() {
        androidExtension.sourceSets.each { sourceSet ->
            if (sourceDirectorySetMap.containsKey(sourceSet.name)) {
                return
            }
            def include = "**/*.pilar"
            sourceSet.java.filter.include(include)
            def dirSetFactory = new DefaultSourceDirectorySetFactory(fileResolver, new DefaultDirectoryFileTreeFactory())
            sourceSet.convention.plugins.jawa = new DefaultJawaSourceSet(sourceSet.name + "_AndroidJawaPlugin", dirSetFactory)
            def jawa = sourceSet.jawa
            jawa.filter.include(include)
            def jawaSrcDir = ["src", sourceSet.name, "jawa"].join(File.separator)
            jawa.srcDir(jawaSrcDir)
            sourceDirectorySetMap[sourceSet.name] = jawa
        }
    }

    /**
     * Updates AndroidPlugin's compilation task to support jawa.
     *
     * @param task the JavaCompile task
     */
    void addAndroidJawaCompileTask(Object variant) {
        def javaCompileTask = variant.javaCompile
        // To prevent locking classes.jar by JDK6's URLClassLoader
        def libraryClasspath = javaCompileTask.classpath.grep { it.name != "classes.jar" }
        def jawaVersion = jawaVersionFromClasspath(libraryClasspath)
        if (!jawaVersion) {
            return
        }
        project.logger.info("jawa-compiler version=$jawaVersion detected")
        def compilerConfigurationName = "androidJawaPluginJawaCompilerFor" + javaCompileTask.name
        def compilerConfiguration = project.configurations.findByName(compilerConfigurationName)
        if (!compilerConfiguration) {
            compilerConfiguration = project.configurations.create(compilerConfigurationName)
            project.dependencies.add(compilerConfigurationName, "com.github.arguslab:jawa-compiler_2.11:$jawaVersion")
        }
        def variantWorkDir = getVariantWorkDir(variant)
        def jawaCompileTask = project.tasks.create("compile${variant.name.capitalize()}Jawa", JawaCompile)
        def jawaSources = variant.variantData.variantConfiguration.sortedSourceProviders.inject([]) { acc, val ->
            acc + val.java.sourceFiles
        }
        jawaCompileTask.source = jawaSources
        jawaCompileTask.setDestinationDir(javaCompileTask.destinationDir)
        jawaCompileTask.setSourceCompatibility(javaCompileTask.sourceCompatibility)
        jawaCompileTask.setTargetCompatibility(javaCompileTask.targetCompatibility)
        jawaCompileTask.setClasspath(javaCompileTask.classpath + project.files(androidPlugin.androidBuilder.getBootClasspath(false)))
        jawaCompileTask.setJawaClasspath(compilerConfiguration.asFileTree)

        def dummyDestinationDir = new File(variantWorkDir, "javaCompileDummyDestination") // TODO: More elegant way
        def dummySourceDir = new File(variantWorkDir, "javaCompileDummySource") // TODO: More elegant way
        def javaCompileOriginalDestinationDir = new AtomicReference<File>()
        def javaCompileOriginalSource = new AtomicReference<FileCollection>()
        def javaCompileOriginalOptionsCompilerArgs = new AtomicReference<List<String>>()
        javaCompileTask.doFirst {
            // Disable compilation
            javaCompileOriginalDestinationDir.set(javaCompileTask.destinationDir)
            javaCompileOriginalSource.set(javaCompileTask.source)
            javaCompileTask.destinationDir = dummyDestinationDir
            if (!dummyDestinationDir.exists()) {
                FileUtils.forceMkdir(dummyDestinationDir)
            }
            def dummySourceFile = new File(dummySourceDir, "Dummy.java")
            if (!dummySourceFile.exists()) {
                FileUtils.forceMkdir(dummySourceDir)
                dummySourceFile.withWriter { it.write("class Dummy{}") }
            }
            javaCompileTask.source = [dummySourceFile]
            def compilerArgs = javaCompileTask.options.compilerArgs
            javaCompileOriginalOptionsCompilerArgs.set(compilerArgs)
            javaCompileTask.options.compilerArgs = compilerArgs +  "-proc:none"
        }
        javaCompileTask.outputs.upToDateWhen { false }
        javaCompileTask.doLast {
            FileUtils.deleteDirectory(dummyDestinationDir)
            javaCompileTask.destinationDir = javaCompileOriginalDestinationDir.get()
            javaCompileTask.source = javaCompileOriginalSource.get()
            javaCompileTask.options.compilerArgs = javaCompileOriginalOptionsCompilerArgs.get()

            // R.java is appended lazily
            jawaCompileTask.source = [] + new TreeSet(jawaCompileTask.source.collect { it } + javaCompileTask.source.collect { it }) // unique
            def noisyProperties = ["compiler", "includeJavaRuntime"]
            InvokerHelper.setProperties(jawaCompileTask.options,
                    javaCompileTask.options.properties.findAll { !noisyProperties.contains(it.key) })
            noisyProperties.each { property ->
                // Suppress message from deprecated/experimental property as possible
                if (!javaCompileTask.options.hasProperty(property) || !jawaCompileTask.options.hasProperty(property)) {
                    return
                }
                if (jawaCompileTask.options[property] != javaCompileTask.options[property]) {
                    jawaCompileTask.options[property] = javaCompileTask.options[property]
                }
            }
            jawaCompileTask.execute()
            project.logger.lifecycle(jawaCompileTask.path)
        }
    }
}