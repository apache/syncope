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

import org.apache.syncope.installer.utilities.FileSystemUtils;
import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.syncope.installer.files.Pom;
import org.apache.syncope.installer.utilities.InstallLog;
import org.apache.syncope.installer.utilities.MavenUtils;
import org.xml.sax.SAXException;

public class ArchetypeProcess {

    public void run(final AbstractUIProcessHandler handler, final String[] args) {
        final String installPath = args[0];
        final String mavenDir = args[1];
        final String groupId = args[2];
        final String artifactId = args[3];
        final String secretKey = args[4];
        final String anonymousKey = args[5];
        final String confDirectory = args[6];
        final String logsDirectory = args[7];
        final String bundlesDirectory = args[8];
        final String syncopeVersion = args[9];
        final String syncopeAdminPassword = args[10];
        final boolean isProxyEnabled = Boolean.valueOf(args[11]);
        final String proxyHost = args[12];
        final String proxyPort = args[13];
        final String proxyUser = args[14];
        final String proxyPwd = args[15];
        final boolean mavenProxyAutoconf = Boolean.valueOf(args[16]);

        final FileSystemUtils fileSystemUtils = new FileSystemUtils(handler);
        fileSystemUtils.createDirectory(installPath);
        InstallLog.initialize(installPath, handler);
        final MavenUtils mavenUtils = new MavenUtils(mavenDir, handler);
        File customMavenProxySettings = null;
        try {
            if (isProxyEnabled && mavenProxyAutoconf) {
                customMavenProxySettings = MavenUtils.createSettingsWithProxy(installPath, proxyHost, proxyPort,
                        proxyUser, proxyPwd);
            }
        } catch (IOException ex) {
            final StringBuilder messageError = new StringBuilder(
                    "I/O error during creation of Maven custom settings.xml");
            final String emittedError = messageError.toString();
            handler.emitError(emittedError, emittedError);
            InstallLog.getInstance().error(messageError.append(ex.getMessage() == null ? "" : ex.getMessage()).
                    toString());
        } catch (ParserConfigurationException ex) {
            final StringBuilder messageError = new StringBuilder(
                    "Parser configuration error during creation of Maven custom settings.xml");
            final String emittedError = messageError.toString();
            handler.emitError(emittedError, emittedError);
            InstallLog.getInstance().error(messageError.append(ex.getMessage() == null ? "" : ex.getMessage()).
                    toString());
        } catch (TransformerException ex) {
            final StringBuilder messageError = new StringBuilder(
                    "Transformer error during creation of Maven custom settings.xml");
            final String emittedError = messageError.toString();
            handler.emitError(emittedError, emittedError);
            InstallLog.getInstance().error(messageError.append(ex.getMessage() == null ? "" : ex.getMessage()).
                    toString());
        } catch (SAXException ex) {
            final StringBuilder messageError = new StringBuilder(
                    "XML parsing error during creation of Maven custom settings.xml");
            final String emittedError = messageError.toString();
            handler.emitError(emittedError, emittedError);
            InstallLog.getInstance().error(messageError.append(ex.getMessage() == null ? "" : ex.getMessage()).
                    toString());
        }
        mavenUtils.archetypeGenerate(
                syncopeVersion, groupId, artifactId, secretKey, anonymousKey, installPath, customMavenProxySettings);

        fileSystemUtils.writeToFile(new File(installPath + "/" + artifactId + Pom.PATH),
                String.format(Pom.FILE, syncopeVersion, syncopeVersion, groupId, artifactId));
        fileSystemUtils.createDirectory(confDirectory);
        fileSystemUtils.createDirectory(logsDirectory);
        fileSystemUtils.createDirectory(bundlesDirectory);
        mavenUtils.createPackage(installPath + "/" + artifactId, confDirectory, logsDirectory, bundlesDirectory,
                customMavenProxySettings);
    }
}
