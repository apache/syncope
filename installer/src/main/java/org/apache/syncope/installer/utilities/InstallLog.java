/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.installer.utilities;

import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.File;
import java.util.Date;

public class InstallLog {

    private static InstallLog installLog = null;

    private final File log;

    private final FileSystemUtils fileSystemUtils;

    private InstallLog(final String installPath, final AbstractUIProcessHandler handler) {
        log = new File(installPath + "/install.log");
        fileSystemUtils = new FileSystemUtils(handler);
    }

    public static InstallLog initialize(final String installPath, final AbstractUIProcessHandler handler) {
        synchronized (InstallLog.class) {
            if (installLog == null) {
                installLog = new InstallLog(installPath, handler);
            }
        }
        return installLog;
    }

    public static InstallLog getInstance() {
        return installLog;
    }

    public void error(final String msg) {
        writeToFile("Error", msg);
    }

    public void info(final String msg) {
        writeToFile("Info", msg);
    }

    private void writeToFile(final String what, final String msg) {
        fileSystemUtils.appendToFile(log, new Date() + " | " + what + ": " + msg);
    }

}
