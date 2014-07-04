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
package org.apache.syncope.installer.processes;

import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class AbstractProcess {

    protected void exec(final String cmd, final AbstractUIProcessHandler handler, final String path) {
        try {
            final ProcessBuilder builder = new ProcessBuilder(cmd.split(" "));
            if (path != null && !path.isEmpty()) {
                builder.directory(new File(path));
            }
            final Process process = builder.start();
            readResponse(process.getInputStream(), handler);
        } catch (IOException ex) {
        }
    }

    protected void readResponse(final InputStream inputStream, final AbstractUIProcessHandler handler) throws
            IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();
        while (line != null) {
            line = reader.readLine();
            handler.logOutput(line == null ? "" : line, false);
        }
        inputStream.close();
    }

    protected void writeToFile(final File orm, final String content) {
        try {
            final FileWriter fw = new FileWriter(orm.getAbsoluteFile());
            final BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException ex) {
        }
    }

}
