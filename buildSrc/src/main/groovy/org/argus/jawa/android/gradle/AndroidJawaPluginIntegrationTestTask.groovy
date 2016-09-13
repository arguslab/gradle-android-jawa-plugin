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

import com.google.common.io.ByteStreams
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * @author <a href="mailto:fgwei521@gmail.com">Fengguo Wei</a>
 */
class AndroidJawaPluginIntegrationTestTask extends DefaultTask {
    @TaskAction
    def run() {
        def travis = System.getenv("TRAVIS").toString().toBoolean()
        [
                ["app", true],
//                ["lib", false],
//                ["appAndLib", true],
//                ["largeAppAndLib", false],
//                ["noScala", false],
//                ["simpleFlavor", false],
//                ["useScalaOnlyTest", false],
//                ["apt", false],
        ].each { projectName, runOnTravis ->
            def gradleArgs = ["clean", "connectedCheck", "uninstallAll"]
            [
                    ["2.10", true,  "1.0.2", "2.0.0", "android-23", "23.0.1", "8", "23"],
            ].each { testParameters ->
                if (!travis || (runOnTravis && testParameters[1])) {
                    def gradleVersion = testParameters[0]
                    def gradleWrapperProperties = getGradleWrapperProperties(gradleVersion)
                    def gradleProperties = getGradleProperties(testParameters.drop(2))
                    println "Test $gradleArgs gradleVersion:$gradleVersion $gradleProperties"
                    runProject(projectName, gradleArgs, gradleWrapperProperties, gradleProperties)
                }
            }
        }
    }

    def getGradleWrapperProperties(gradleVersion) {
        def gradleWrapperProperties = new Properties()
        gradleWrapperProperties.putAll([
                distributionBase: "GRADLE_USER_HOME",
                distributionPath: "wrapper/dists",
                zipStoreBase: "GRADLE_USER_HOME",
                zipStorePath: "wrapper/dists",
                distributionUrl: "http://services.gradle.org/distributions/gradle-" + gradleVersion + "-bin.zip",
        ])
        gradleWrapperProperties
    }

    def getGradleProperties(jawaVersion, androidPluginVersion, androidPluginCompileSdkVersion,
                            androidPluginBuildToolsVersion, androidPluginMinSdkVersion, androidPluginTargetSdkVersion) {
        def snaphotRepositoryUrl = [project.buildFile.parentFile.absolutePath, "gh-pages", "repository", "snapshot"].join(File.separator)
        def gradleProperties = new Properties()
        gradleProperties.putAll([
                "org.gradle.jvmargs": "-Xmx2048m -XX:MaxPermSize=2048m -XX:+HeapDumpOnOutOfMemoryError",
                snaphotRepositoryUrl: snaphotRepositoryUrl,
                jawaVersion: jawaVersion,
                androidJawaPluginVersion: "1.0.1-SNAPSHOT",
                androidPluginVersion: androidPluginVersion,
                androidPluginCompileSdkVersion: androidPluginCompileSdkVersion,
                androidPluginBuildToolsVersion: androidPluginBuildToolsVersion,
                androidPluginMinSdkVersion: androidPluginMinSdkVersion,
                androidPluginTargetSdkVersion: androidPluginTargetSdkVersion,
                androidPluginIncremental: "false",
                androidPluginPreDexLibraries: "false",
                androidPluginJumboMode: "false",
        ])
        gradleProperties
    }

    def runProject(projectName, tasks, gradleWrapperProperties, gradleProperties) {
        def baseDir = new File([project.buildFile.parentFile.absolutePath, "src", "integTest"].join(File.separator))
        def projectDir = new File([baseDir.absolutePath, projectName].join(File.separator))
        new File(projectDir, ["gradle", "wrapper", "gradle-wrapper.properties"].join(File.separator)).withWriter {
            gradleWrapperProperties.store(it, getClass().getName())
        }
        new File(projectDir, "gradle.properties").withWriter {
            gradleProperties.store(it, getClass().getName())
        }
        def gradleWrapper = new GradleWrapper(projectDir)
        def args = ["--no-daemon", "--stacktrace", "-Pcom.android.build.threadPoolSize=5"] + tasks
        println "gradlew $args"
        def process = gradleWrapper.execute(args)
        [Thread.start { ByteStreams.copy(process.in, System.out) },
         Thread.start { ByteStreams.copy(process.err, System.err) }].each { it.join() }
        process.waitFor()
        // process.waitForProcessOutput(System.out, System.err)
        if (process.exitValue() != 0) {
            throw new IOException("process.exitValue != 0 but ${process.exitValue()}")
        }
    }
}