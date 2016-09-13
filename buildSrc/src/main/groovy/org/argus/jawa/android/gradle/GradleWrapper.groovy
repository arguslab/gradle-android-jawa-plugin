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

import org.apache.tools.ant.taskdefs.condition.Os

/**
 * @author <a href="mailto:fgwei521@gmail.com">Fengguo Wei</a>
 */
class GradleWrapper {
    private final File dir

    public GradleWrapper(File dir) {
        this.dir = dir
    }

    public Process execute(List<String> options) {
        def ext = (Os.isFamily(Os.FAMILY_WINDOWS) ? ".bat" : "")
        def gradlew = dir.absolutePath + File.separator + "gradlew" + ext
        def processBuilder = new ProcessBuilder([gradlew] + options)
        processBuilder.directory(dir)
        processBuilder.start()
    }
}
