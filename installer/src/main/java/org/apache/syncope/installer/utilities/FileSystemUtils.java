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

public class FileSystemUtils {

    public static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("win");

    private final AbstractUIProcessHandler handler;
    
    public FileSystemUtils(final AbstractUIProcessHandler handler) {
        this.handler = handler;
    }
    
    public void createDirectory(final String directoryPath, final String path) {
        exec(String.format(CREATE_DIRECTORY, directoryPath), path);
    }

    private static final String CREATE_DIRECTORY = "mkdir -p %s";

    public void exec(final String cmd, final String path) {
        try {
            final ProcessBuilder builder = new ProcessBuilder(cmd.split(" "));
            if (path != null && !path.isEmpty()) {
                builder.directory(new File(path));
            }
            final Process process = builder.start();
            readResponse(process.getInputStream());
        } catch (final IOException ex) {
            handler.emitError("Error executing " + cmd + ": " + ex.getMessage(),
                    "Error executing " + cmd + ": " + ex.getMessage());
        }
    }

    private void readResponse(final InputStream inputStream) throws
            IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();
        while (line != null) {
            line = reader.readLine();
            handler.logOutput(line == null ? "" : line, false);
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
            handler.emitError("Error writing file" + file.getAbsolutePath() + ": " + ex.getMessage(),
                    "Error writing file" + file.getAbsolutePath() + ": " + ex.getMessage());
        }
    }

}
