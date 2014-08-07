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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

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
            final String artifactId, final String secretKey, final String anonymousKey, final String installPath) {

        final InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Collections.singletonList("archetype:generate"));
        request.setInteractive(false);
        request.setProperties(archetypeProperties(archetypeVersion, groupId, artifactId, secretKey, anonymousKey));
        invoke(request, installPath);
    }

    private Properties archetypeProperties(final String archetypeVersion, final String groupId,
            final String artifactId, final String secretKey, final String anonymousKey) {
        final Properties properties = new Properties();
        properties.setProperty("archetypeGroupId", "org.apache.syncope");
        properties.setProperty("archetypeArtifactId", "syncope-archetype");
        properties.setProperty("archetypeRepository", "http://repository.apache.org/content/repositories/snapshots");
        properties.setProperty("archetypeVersion", archetypeVersion);
        properties.setProperty("groupId", groupId);
        properties.setProperty("artifactId", artifactId);
        properties.setProperty("secretKey", secretKey);
        properties.setProperty("anonymousKey", anonymousKey);
        return properties;
    }

    public void createPackage(final String path, final String confDirectory,
            final String logDirectory, final String bundlesDirectory) {

        final InvocationRequest request = new DefaultInvocationRequest();
        request.setProperties(packageProperties(confDirectory, logDirectory, bundlesDirectory));
        final List<String> mavenGoals = new ArrayList<String>();
        mavenGoals.add("clean");
        mavenGoals.add("package");
        request.setGoals(mavenGoals);
        invoke(request, path);
    }

    private Properties packageProperties(final String confDirectory, final String logDirectory,
            final String bundlesDirectory) {
        final Properties properties = new Properties();
        properties.setProperty("conf.directory", confDirectory);
        properties.setProperty("log.directory", logDirectory);
        properties.setProperty("bundles.directory", bundlesDirectory);
        return properties;
    }

    private InvocationResult invoke(final InvocationRequest request, final String path) {
        InvocationResult result = null;
        final Invoker invoker = new DefaultInvoker();
        invoker.setOutputHandler(null);
        invoker.setWorkingDirectory(new File(path));
        try {
            result = invoker.execute(request);
        } catch (MavenInvocationException ex) {
            handler.emitError("Maven exception: " + ex.getMessage(), "Maven exception: " + ex.getMessage());
        }
        return result;
    }

}
