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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class FileSystemUtils {

    public static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("win");

    private static final String UNIX_CREATE_DIRECTORY = "mkdir -p %s";

    private static final String WIN_CREATE_DIRECTORY = "mkdir %s";

    private final AbstractUIProcessHandler handler;

    public FileSystemUtils(final AbstractUIProcessHandler handler) {
        this.handler = handler;
    }

    public void createDirectory(final String directoryPath, final String path) {
        if (IS_WIN) {
            exec(String.format(WIN_CREATE_DIRECTORY, directoryPath), path);
        } else {
            exec(String.format(UNIX_CREATE_DIRECTORY, directoryPath), path);
        }

    }

    public void exec(final String cmd, final String path) {
        try {
            handler.logOutput("Executing " + cmd, true);
            final ProcessBuilder builder = new ProcessBuilder(cmd.split(" "));
            if (path != null && !path.isEmpty()) {
                builder.directory(new File(path));
            }
            final Process process = builder.start();
            readResponse(process.getInputStream());
        } catch (final IOException ex) {
            final String errorMessage = "Error executing " + cmd + ": " + ex.getMessage();
            handler.emitError(errorMessage, errorMessage);
            InstallLog.getInstance().error(errorMessage);
        }
    }

    private void readResponse(final InputStream inputStream) throws
            IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();
        while (line != null) {
            line = reader.readLine();
            final String content = line == null ? "" : line;
            handler.logOutput(content, false);
            InstallLog.getInstance().info(content);
        }
        inputStream.close();
    }

    public void writeToFile(final File file, final String content) {
        try {
            final FileWriter fw = new FileWriter(file.getAbsoluteFile());
            final BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (final IOException ex) {
            final String errorMessage = "Error writing file " + file.getAbsolutePath() + ": " + ex.getMessage();
            handler.emitError(errorMessage, errorMessage);
            InstallLog.getInstance().error(errorMessage);
        }
    }

    public void appendToFile(final File file, final String content) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            final PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            out.println(content);
            out.close();
        } catch (IOException ex) {
            final String errorMessage = "Error writing file " + file.getAbsolutePath() + ": " + ex.getMessage();
            handler.emitError(errorMessage, errorMessage);
            InstallLog.getInstance().error(errorMessage);
        }
    }

}
