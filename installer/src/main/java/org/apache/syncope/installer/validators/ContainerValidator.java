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
package org.apache.syncope.installer.validators;

import com.izforge.izpack.api.data.InstallData;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.installer.enums.Containers;
import org.apache.syncope.installer.utilities.HttpUtils;

public class ContainerValidator extends AbstractValidator {

    private StringBuilder error;

    @Override
    public Status validateData(final InstallData installData) {

        final Containers selectedContainer = Containers.fromContainerName(
                installData.getVariable("install.container.selection"));
        final String tomcatSsl = installData.getVariable("tomcat.container.ssl");
        final String tomcatHost = installData.getVariable("tomcat.container.host");
        final String tomcatPort = installData.getVariable("tomcat.container.port");
        final String tomcatUser = installData.getVariable("tomcat.container.user");
        final String tomcatPassword = installData.getVariable("tomcat.container.pwd");
        final String glassfishDir = installData.getVariable("glassfish.container.dir");
        final String jbossSsl = installData.getVariable("jboss.container.ssl");
        final String jbossHost = installData.getVariable("jboss.container.host");
        final String jbossPort = installData.getVariable("jboss.container.port");
        final String jbossJdbcModule = installData.getVariable("jboss.container.jdbc.module");
        final String jbossAdminUsername = installData.getVariable("jboss.container.user");
        final String jbossAdminPassword = installData.getVariable("jboss.container.pwd");

        switch (selectedContainer) {
            case TOMCAT:

                boolean verified = true;
                error = new StringBuilder("Required fields:\n");
                if (StringUtils.isBlank(tomcatHost)) {
                    error.append("Tomcat host\n");
                    verified = false;
                }
                if (StringUtils.isBlank(tomcatPort)) {
                    error.append("Tomcat port\n");
                    verified = false;
                }
                if (StringUtils.isBlank(tomcatUser)) {
                    error.append("Tomcat user\n");
                    verified = false;
                }
                if (StringUtils.isBlank(tomcatPassword)) {
                    error.append("Tomcat password\n");
                    verified = false;
                }

                if (!verified) {
                    return Status.ERROR;
                }

                int responseCode = HttpUtils.ping(Boolean.valueOf(tomcatSsl), tomcatHost, tomcatPort);

                if (responseCode == 200) {
                    return Status.OK;
                } else {
                    error = new StringBuilder("Tomcat URL is offline");
                    return Status.ERROR;
                }
            case JBOSS:
                boolean virified = true;
                error = new StringBuilder("Required fields:\n");
                if (StringUtils.isBlank(jbossHost)) {
                    error.append("JBoss Host\n");
                    virified = false;
                }
                if (StringUtils.isBlank(jbossPort)) {
                    error.append("JBoss Port\n");
                    virified = false;
                }
                if (StringUtils.isBlank(jbossJdbcModule)) {
                    error.append("JBoss JDBC module name\n");
                    virified = false;
                }
                if (StringUtils.isBlank(jbossAdminUsername)) {
                    error.append("JBoss admin username\n");
                    virified = false;
                }
                if (StringUtils.isBlank(jbossAdminPassword)) {
                    error.append("JBoss admin password\n");
                    virified = false;
                }

                if (!virified) {
                    return Status.ERROR;
                }

                int jResponseCode = HttpUtils.ping(Boolean.valueOf(jbossSsl), jbossHost, jbossPort);

                if (jResponseCode == 200) {
                    return Status.OK;
                } else {
                    error = new StringBuilder("JBoss URL is offline");
                    return Status.ERROR;
                }
            case GLASSFISH:
                error = new StringBuilder("Required fields:\n");
                if (StringUtils.isBlank(glassfishDir)) {
                    error.append("Glassfish directory\n");
                    return Status.ERROR;
                }

                final File dir = new File(glassfishDir);

                if (!dir.exists()) {
                    error.append("Glassfish directory not found");
                    return Status.ERROR;
                }

                if (!dir.isDirectory()) {
                    error.append("Glassfish directory is not a directory");
                    return Status.ERROR;
                }
                return Status.OK;
            default:
                error = new StringBuilder("Container not supported yet");
                return Status.ERROR;
        }
    }

    @Override
    public String getErrorMessageId() {
        return error.toString();
    }

    @Override
    public String getWarningMessageId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getDefaultAnswer() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
