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
package org.apache.syncope.client.cli.commands.install;

import java.util.ResourceBundle;
import org.apache.commons.lang3.SystemUtils;

public final class InstallConfigFileTemplate {

    private static final ResourceBundle CONF = ResourceBundle.getBundle("configuration");

    public static final String CONFIGURATION_FILE_NAME = CONF.getString("cli.installation.filename");

    private static final String SYNCOPE_REST_SERVICES = "syncope.rest.services=%s://%s:%s%s";

    private static final String SYNCOPE_ADMIN_USER = "syncope.admin.user=%s";

    private static final String SYNCOPE_ADMIN_PASSWORD = "syncope.admin.password=%s";

    public static String cliPropertiesFile(
            final String schema,
            final String hostname,
            final String port,
            final String restContext,
            final String user,
            final String password) {

        final String syncopeRestServices = String.format(SYNCOPE_REST_SERVICES, schema, hostname, port, restContext);
        final String syncopeAdminUser = String.format(SYNCOPE_ADMIN_USER, user);
        final String syncopeAdminPassword = String.format(SYNCOPE_ADMIN_PASSWORD, password);
        final String useGZIPCompression = String.format("syncope.useGZIPCompression=true");

        return syncopeRestServices + "\n" + syncopeAdminUser + "\n" + syncopeAdminPassword + "\n" + useGZIPCompression;
    }

    public static String dirPath() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return CONF.getString("cli.installation.directory.windows");
        } else {
            return CONF.getString("cli.installation.directory.linux");
        }
    }

    public static String configurationFilePath() {
        return dirPath() + CONFIGURATION_FILE_NAME;
    }

    private static String scriptFileName() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return CONF.getString("script.file.name.windows");
        } else {
            return CONF.getString("script.file.name.linux");
        }
    }

    public static String scriptFilePath() {
        return dirPath() + scriptFileName();
    }

    private InstallConfigFileTemplate() {
    }
}
