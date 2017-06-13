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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

public class FileSystemUtils {

    private final AbstractUIProcessHandler handler;

    public FileSystemUtils(final AbstractUIProcessHandler handler) {
        this.handler = handler;
    }

    public void createDirectory(final String directoryPath) {
        final File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public void copyFile(final String sourceFilePath, final String targetFilePath) {
        try {
            FileUtils.copyFile(new File(sourceFilePath), new File(targetFilePath));
        } catch (final IOException ex) {
            final String errorMessage =
                    "Error copying file " + sourceFilePath + " to " + targetFilePath + ": " + ex.getMessage();
            handler.emitError(errorMessage, errorMessage);
            InstallLog.getInstance().error(errorMessage);
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

    private void readResponse(final InputStream inputStream) throws IOException {
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
            try (BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(content);
            }
        } catch (final IOException ex) {
            final String errorMessage = "Error writing file " + file.getAbsolutePath() + ": " + ex.getMessage();
            handler.emitError(errorMessage, errorMessage);
            InstallLog.getInstance().error(errorMessage);
        }
    }

    public String readFile(final File file) {
        String content = "";
        try {
            content = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
        } catch (IOException ex) {
            final String errorMessage = "Error reading file " + file.getAbsolutePath() + ": " + ex.getMessage();
            handler.emitError(errorMessage, errorMessage);
            InstallLog.getInstance().error(errorMessage);
        }
        return content;
    }

    public void appendToFile(final File file, final String content) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
                out.println(content);
            }
        } catch (IOException ex) {
            final String errorMessage = "Error writing file " + file.getAbsolutePath() + ": " + ex.getMessage();
            handler.emitError(errorMessage, errorMessage);
            InstallLog.getInstance().error(errorMessage);
        }
    }

    public static void writeXML(final Document doc, final OutputStream out) throws IOException, TransformerException {
        try {
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(doc),
                    new StreamResult(new OutputStreamWriter(out, Charset.forName("UTF-8"))));
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public static void delete(final File file) {
        FileUtils.deleteQuietly(file);
    }

    public void copyFileFromResources(
            final String filePath, final String destination, final AbstractUIProcessHandler handler) {

        try {
            FileUtils.copyURLToFile(getClass().getResource(filePath), new File(destination));
        } catch (IOException ex) {
            String errorMessage = "Error copying file " + filePath + " to + " + destination + ": " + ex.getMessage();
            handler.emitError(errorMessage, errorMessage);
            InstallLog.getInstance().error(errorMessage);
        }
    }
}
