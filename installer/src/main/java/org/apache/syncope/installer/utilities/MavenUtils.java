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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class MavenUtils {

    private static final String MAVEN_HOME_PROPERTY = "maven.home";

    private final AbstractUIProcessHandler handler;

    public MavenUtils(final String mavenHomeDirectory, final AbstractUIProcessHandler handler) {
        if (System.getProperty(MAVEN_HOME_PROPERTY) == null || System.getProperty(MAVEN_HOME_PROPERTY).isEmpty()) {
            System.setProperty(MAVEN_HOME_PROPERTY, mavenHomeDirectory);
        }
        this.handler = handler;
    }

    public void archetypeGenerate(final String archetypeVersion, final String groupId,
            final String artifactId, final String secretKey, final String anonymousKey, final String installPath,
            final File customSettingsFile) {

        final InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Collections.singletonList("archetype:generate"));
        request.setBatchMode(true);
        final Properties properties =
                archetypeProperties(archetypeVersion, groupId, artifactId, secretKey, anonymousKey);
        request.setProperties(properties);
        if (customSettingsFile != null && FileUtils.sizeOf(customSettingsFile) > 0) {
            request.setUserSettingsFile(customSettingsFile);
        }
        logToHandler(request.getGoals(), properties);
        logToFile(request.getGoals(), properties);
        invoke(request, installPath);
    }

    private Properties archetypeProperties(final String archetypeVersion, final String groupId,
            final String artifactId, final String secretKey, final String anonymousKey) {
        final Properties properties = new Properties();
        properties.setProperty("archetypeGroupId", "org.apache.syncope");
        properties.setProperty("archetypeArtifactId", "syncope-archetype");
        if (archetypeVersion.contains("SNAPSHOT")) {
            properties.setProperty("archetypeRepository",
                    "http://repository.apache.org/content/repositories/snapshots");
        } else {
            properties.setProperty("archetypeRepository", "http://repo1.maven.org/maven2");
        }
        properties.setProperty("archetypeVersion", archetypeVersion);
        properties.setProperty("groupId", groupId);
        properties.setProperty("artifactId", artifactId);
        properties.setProperty("secretKey", secretKey);
        properties.setProperty("anonymousKey", anonymousKey);
        properties.setProperty("version", "1.0-SNAPSHOT");
        return properties;
    }

    public void mvnCleanPackageWithProperties(
            final String path, final Properties properties, final File customSettingsFile) {

        final InvocationRequest request = new DefaultInvocationRequest();
        request.setProperties(properties);
        if (customSettingsFile != null && FileUtils.sizeOf(customSettingsFile) > 0) {
            request.setUserSettingsFile(customSettingsFile);
        }
        final List<String> mavenGoals = new ArrayList<>();
        mavenGoals.add("clean");
        mavenGoals.add("package");
        request.setGoals(mavenGoals);
        logToHandler(request.getGoals(), properties);
        logToFile(request.getGoals(), properties);
        invoke(request, path);
    }

    private void logToHandler(final List<String> goals, final Properties properties) {
        handler.logOutput("Executing maven command:", true);
        final StringBuilder mavenCommand = new StringBuilder("mvn ");
        for (final String goal : goals) {
            mavenCommand.append(goal).append(" ");
        }
        handler.logOutput(mavenCommand.toString(), true);
        for (final String propertyName : properties.stringPropertyNames()) {
            handler.logOutput("-D " + propertyName + "=" + properties.getProperty(propertyName), true);
        }
    }

    private void logToFile(final List<String> goals, final Properties properties) {
        InstallLog.getInstance().info("Executing maven command:");
        final StringBuilder mavenCommand = new StringBuilder("mvn ");
        for (final String goal : goals) {
            mavenCommand.append(goal).append(" ");
        }
        InstallLog.getInstance().info(mavenCommand.toString());
        for (final String propertyName : properties.stringPropertyNames()) {
            InstallLog.getInstance().info("-D " + propertyName + "=" + properties.getProperty(propertyName));
        }
    }

    private InvocationResult invoke(final InvocationRequest request, final String path) {
        InvocationResult result = null;
        final Invoker invoker = new DefaultInvoker();
        try {
            invoker.setLogger(new PrintStreamLogger(
                    new PrintStream(InstallLog.getInstance().getFileAbsolutePath()), 1000));
            invoker.setOutputHandler(new PrintStreamHandler(
                    new PrintStream(InstallLog.getInstance().getFileAbsolutePath()), true));
            invoker.setWorkingDirectory(new File(path));
            result = invoker.execute(request);
        } catch (MavenInvocationException | FileNotFoundException ex) {
            final String messageError = "Maven exception: " + ex.getMessage();
            handler.emitError(messageError, messageError);
            InstallLog.getInstance().info(messageError);
        }
        return result;
    }

    public static File createSettingsWithProxy(final String path, final String proxyHost, final String proxyPort,
            final String proxyUser, final String proxyPassword) throws IOException, ParserConfigurationException,
            TransformerException, SAXException {
        final File settingsXML = new File(System.getProperty(MAVEN_HOME_PROPERTY) + (System.getProperty(
                MAVEN_HOME_PROPERTY).endsWith("/") ? "conf/settings.xml" : "/conf/settings.xml"));
        final File tempSettingsXML = new File(path + (path.endsWith("/") ? "settings_temp.xml" : "/settings_temp.xml"));
        if (settingsXML.canRead() && !tempSettingsXML.exists()) {
            tempSettingsXML.createNewFile();

            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final DocumentBuilder builder = dbf.newDocumentBuilder();
            // parse settings.xml
            final Document settings = builder.parse(settingsXML);

            final Element proxies = (Element) settings.getDocumentElement().getElementsByTagName("proxies").item(0);

            final Element proxy = settings.createElement("proxy");

            final Element id = settings.createElement("id");
            final Element active = settings.createElement("active");
            final Element protocol = settings.createElement("protocol");
            final Element host = settings.createElement("host");
            final Element port = settings.createElement("port");
            final Element nonProxyHosts = settings.createElement("nonProxyHosts");
            id.appendChild(settings.createTextNode("optional"));
            active.appendChild(settings.createTextNode("true"));
            protocol.appendChild(settings.createTextNode("http"));
            host.appendChild(settings.createTextNode(proxyHost));
            port.appendChild(settings.createTextNode(proxyPort));
            proxy.appendChild(id);
            proxy.appendChild(active);
            proxy.appendChild(protocol);
            // create username and password tags only if required
            if (proxyUser != null && !proxyUser.isEmpty() && proxyPassword != null) {
                final Element username = settings.createElement("username");
                final Element password = settings.createElement("password");
                username.appendChild(settings.createTextNode(proxyUser));
                password.appendChild(settings.createTextNode(proxyPassword));
                proxy.appendChild(username);
                proxy.appendChild(password);
            }
            proxy.appendChild(host);
            proxy.appendChild(port);
            proxy.appendChild(nonProxyHosts);

            proxies.appendChild(proxy);

            FileSystemUtils.writeXML(settings, new FileOutputStream(tempSettingsXML));

        }
        return tempSettingsXML;
    }
}
